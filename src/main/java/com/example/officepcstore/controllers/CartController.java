package com.example.officepcstore.controllers;

import com.example.officepcstore.excep.AppException;
import com.example.officepcstore.models.enity.User;
import com.example.officepcstore.payload.request.CartReq;
import com.example.officepcstore.security.jwt.JwtUtils;
import com.example.officepcstore.service.CartService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class CartController {

    private final CartService cartService;
    private final JwtUtils jwtUtils;

    @GetMapping(path = "/cart/get/all")
    public ResponseEntity<?> getAllProductFromCart (HttpServletRequest request){
        User user = jwtUtils.getUserFromJWT(jwtUtils.getJwtFromHeader(request));
            return cartService.getProductFromCart(user.getId());
    }

    @PostMapping(path = "/cart/put")
    public ResponseEntity<?> putProductToCart (@RequestBody @Valid CartReq req,
                                                  HttpServletRequest request){
        User user = jwtUtils.getUserFromJWT(jwtUtils.getJwtFromHeader(request));
            return cartService.createAndPutProductToCart(user.getId(), req);

    }

    @DeleteMapping(path = "/cart/remove/{productId}")
    public ResponseEntity<?> removeProductInCart (@PathVariable("productId") String orderItemId,
                                                  HttpServletRequest request){
        User user = jwtUtils.getUserFromJWT(jwtUtils.getJwtFromHeader(request));
            return cartService.removeProductFromCart(user.getId(), orderItemId);
    }
}
