package sales_savvy_backend.controller;

import com.razorpay.RazorpayException;
import sales_savvy_backend.dto.VerifyPaymentRequest;
import sales_savvy_backend.entity.Payment;
import sales_savvy_backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    public Map<String, Object> createPayment(@PathVariable Integer orderId) throws RazorpayException {
        return paymentService.createRazorpayOrder(orderId);
    }

    @PostMapping("/verify")
    public Payment verifyPayment(@RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyAndCompletePayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
    }
}