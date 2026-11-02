package sales_savvy_backend.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sales_savvy_backend.entity.Order;
import sales_savvy_backend.entity.OrderStatus;
import sales_savvy_backend.entity.Payment;
import sales_savvy_backend.entity.PaymentStatus;
import sales_savvy_backend.repository.OrderRepository;
import sales_savvy_backend.repository.PaymentRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    @Transactional
    public Map<String, Object> createRazorpayOrder(Integer orderId, String email) throws RazorpayException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Same message for "missing" and "someone else's" so we do not reveal which orders exist
        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Order not found");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("This order has already been paid or can no longer be paid");
        }

        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // Razorpay expects the amount in paise
        long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcpt_" + order.getId());

        com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("RAZORPAY");
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionId(razorpayOrderId);
        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("razorpayOrderId", razorpayOrderId);
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("keyId", razorpayKeyId);
        return response;
    }

    @Transactional
    public Payment verifyAndCompletePayment(String razorpayOrderId, String razorpayPaymentId,
                                            String razorpaySignature, String email) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> razorpayOrderId != null && razorpayOrderId.equals(p.getTransactionId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        if (!payment.getOrder().getUser().getEmail().equals(email)) {
            throw new RuntimeException("Payment record not found");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        if (isSignatureValid(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(razorpayPaymentId);
            payment.getOrder().setStatus(OrderStatus.APPROVED);
            orderRepository.save(payment.getOrder());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        return paymentRepository.save(payment);
    }

    private boolean isSignatureValid(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
