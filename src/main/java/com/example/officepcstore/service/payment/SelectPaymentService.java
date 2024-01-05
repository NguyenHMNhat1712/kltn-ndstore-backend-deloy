package com.example.officepcstore.service.payment;

import com.example.officepcstore.config.Constant;
import com.example.officepcstore.excep.AppException;
import com.example.officepcstore.excep.NotFoundException;
import com.example.officepcstore.models.enity.Order;
import com.example.officepcstore.models.enity.PaymentOrderMethod;
import com.example.officepcstore.models.enity.Shipment;
import com.example.officepcstore.models.enity.User;
import com.example.officepcstore.payload.request.PayReq;
import com.example.officepcstore.repository.OrderProductRepository;
import com.example.officepcstore.repository.OrderRepository;
import com.example.officepcstore.repository.UserRepository;
import com.example.officepcstore.security.jwt.JwtUtils;
import com.example.officepcstore.utils.PayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelectPaymentService {
    public static String URL_PAYMENT = "https://kltn-nd-store.vercel.app/checkout/order/payment?complete=";
    private final ApplicationContext context;
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final PayUtils payUtils;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    public PaymentSteps getPaymentSteps(String typesPayment) {
        switch (typesPayment) {
            case Constant.PAY_BY_VNPAY: return context.getBean(VnpayService.class);
            case Constant.PAY_BY_COD: return context.getBean(CodService.class);
            case Constant.PAY_BY_PAYPAL: return context.getBean(PaypalService.class);
            default:
                return null;
        }
    }
    @Transactional
    public ResponseEntity<?>  initializationPayment(HttpServletRequest request, String id, String paymentType, PayReq req) {
        Optional<Order> order;
        String userId = jwtUtils.getUserFromJWT(jwtUtils.getJwtFromHeader(request)).getId();
        try {
            order = orderRepository.findOrderByUser_IdAndStatusOrder(new ObjectId(userId), Constant.ORDER_CART);
           // order = orderRepository.findOrderByUser_Id(new ObjectId(userId));
            if (order.isEmpty() || !order.get().getId().equals(id)) {
                throw new NotFoundException("Not found any order with id: " + id);
            }
            PaymentOrderMethod paymentOrderMethod = new PaymentOrderMethod(null,paymentType.toUpperCase(), "", new HashMap<>());
//            paymentInformation.getPayDetails().put("invoiceDate", LocalDateTime.now(Clock.systemDefaultZone()));
            order.get().setPaymentOrderMethod(paymentOrderMethod);
            Shipment shipment = new Shipment(req.getName(), req.getPhone(),
                    req.getProvince(), req.getDistrict(), req.getWard(),req.getAddress(),req.getNote());
            order.get().setShipment(shipment);
            order.get().getShipment().getServiceShipDetail().put("totalFeeShip", req.getShipFee());
            order.get().getShipment().getServiceShipDetail().put("serviceType", req.getServiceType());
            order.get().getShipment().getServiceShipDetail().put("estimatedTime", req.getEstimatedTime());
            order.get().setInvoiceDate(LocalDateTime.now());
            order.get().setTotalPriceOrder(order.get().getTotalPrice());
            order.get().setStatusOrder(Constant.ORDER_PROCESS);
            orderRepository.save(order.get());
            payUtils.updateProductPriceOrder(order.get());
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            throw new NotFoundException(e.getMessage());
        }catch (AppException e) {
            throw new AppException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Some thing wrong with cart: "+ userId);
        }
       PaymentSteps paymentSteps = getPaymentSteps(paymentType);
        return paymentSteps.initializationPayment(request, order.get());
    }

    @Transactional
    public ResponseEntity<?> makePayment(String paymentId, String payerPayPalId, String responseCode,
                                            String vnPayId, HttpServletRequest request, HttpServletResponse response) {
       if (responseCode != null) {
           PaymentSteps paymentSteps = getPaymentSteps(Constant.PAY_BY_VNPAY);
           return paymentSteps.makePayment(null, null, responseCode, vnPayId, request, response);
       }
          else if (paymentId != null && payerPayPalId != null ) {
                PaymentSteps paymentSteps = getPaymentSteps(Constant.PAY_BY_PAYPAL);
                return paymentSteps.makePayment(paymentId, payerPayPalId, null,null, request, response);

        } else {
            getRoleToCancel(request);
            PaymentSteps paymentSteps = getPaymentSteps(Constant.PAY_BY_COD);
            return paymentSteps.makePayment(paymentId, null, null,null, request, response);
        }
    }



    @Transactional
    public ResponseEntity<?> cancelPayment(String id, String responseCode, HttpServletRequest request, HttpServletResponse response) {
        String check = id.split("-")[0];
        if (responseCode != null) {
            PaymentSteps paymentSteps = getPaymentSteps(Constant.PAY_BY_VNPAY);
            return paymentSteps.cancelPayment(id, responseCode, response);
        } else if (check.equals("EC")) {
                PaymentSteps paymentSteps = getPaymentSteps(Constant.PAY_BY_PAYPAL);
                return paymentSteps.cancelPayment(id, null, response);
        } else {
            getRoleToCancel(request);
            PaymentSteps paymentSteps = getPaymentSteps(Constant.PAY_BY_COD);
            return paymentSteps.cancelPayment(id, null, response);
        }
    }


    private void getRoleToCancel(HttpServletRequest request) {
        String userId = jwtUtils.getUserFromJWT(jwtUtils.getJwtFromHeader(request)).getId();
        Optional<User> user = userRepository.findUserByIdAndStatusUser(userId, Constant.USER_ACTIVE);
        if (user.isEmpty() || !(user.get().getRole().equals(Constant.ROLE_ADMIN)))
            throw new AppException(HttpStatus.FORBIDDEN.value(), "You don't have permission!");
    }


}
