package sales_savvy_backend.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import sales_savvy_backend.entity.*;
import sales_savvy_backend.repository.OrderRepository;
import sales_savvy_backend.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, Object> createRazorpayOrder(Integer orderId) throws RazorpayException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // Razorpay expects the amount in the smallest currency unit (paise for INR)
        int amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcpt_" + order.getId());

        com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);

        // Create a pending Payment record locally, linked to our order
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("RAZORPAY");
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionId(razorpayOrder.get("id"));
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("razorpayOrderId", razorpayOrder.get("id"));
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("keyId", razorpayKeyId);

        return response;
    }

    public Payment verifyAndCompletePayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> razorpayOrderId.equals(p.getTransactionId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        boolean isValid = verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (isValid) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(razorpayPaymentId);
            payment.getOrder().setStatus(OrderStatus.APPROVED);
            orderRepository.save(payment.getOrder());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        return paymentRepository.save(payment);
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec =
                    new javax.crypto.spec.SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}