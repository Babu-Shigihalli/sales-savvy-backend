package sales_savvy_backend.controller;

import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sales_savvy_backend.dto.VerifyPaymentRequest;
import sales_savvy_backend.entity.Payment;
import sales_savvy_backend.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    public Map<String, Object> createPayment(@PathVariable Integer orderId, Authentication authentication)
            throws RazorpayException {
        return paymentService.createRazorpayOrder(orderId, authentication.getName());
    }

    @PostMapping("/verify")
    public Payment verifyPayment(@RequestBody VerifyPaymentRequest request, Authentication authentication) {
        return paymentService.verifyAndCompletePayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                authentication.getName()
        );
    }
}
