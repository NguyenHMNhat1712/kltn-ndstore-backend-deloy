package com.example.officepcstore.service;

import com.example.officepcstore.excep.AppException;
import com.example.officepcstore.models.enity.Order;
import com.example.officepcstore.payload.request.CreateShipReq;
import com.example.officepcstore.payload.request.ShipReq;
import com.example.officepcstore.utils.HttpConnectTemplate;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
@Service
@Slf4j
public class LogisticService {
    @Value("${app.ghn.token}")
    private String TOKEN;
    @Value("${app.ghn.shop}")
    private String SHOP_ID;

    public ResponseEntity<?> getProvince(){
        try {
            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("master-data/province","", TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get province");
        }
    }

    public ResponseEntity<?> getDistrict(Long provinceId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("province_id", provinceId);
            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("master-data/district",
                    body.toString(), TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get district");
        }
    }

    public ResponseEntity<?> getWard(Long districtId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("district_id", districtId);
            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("master-data/ward?district_id",
                    body.toString(), TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get ward");
        }
    }

    public ResponseEntity<?> getDetail(String id) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("client_order_code", id);
            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("v2/shipping-order/detail-by-client-code",
                    body.toString(), TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get detail");
        }
    }

    public ResponseEntity<?> calculateFee(ShipReq req) {
        if (req.getWeight() > 30000) req.setWeight(30000L);
        if (req.getHeight() > 150) req.setHeight(150L);
        try {
            JsonObject body = new JsonObject();
            body.addProperty("service_type_id", req.getService_type_id());
            body.addProperty("to_district_id", req.getTo_district_id());
            body.addProperty("to_ward_code", req.getTo_ward_code());
            body.addProperty("weight", req.getWeight());
            body.addProperty("length",20);
            body.addProperty("width",10);
            body.addProperty("height",req.getHeight());

            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("v2/shipping-order/fee",
                    body.toString(), TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get fee");
        }
    }

    public ResponseEntity<?> calculateExpectedDeliveryTime(ShipReq req) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("service_id", req.getService_type_id());
            body.addProperty("to_district_id", req.getTo_district_id());
            body.addProperty("to_ward_code", req.getTo_ward_code());

            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("v2/shipping-order/leadtime",
                    body.toString(), TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get expected delivery time");
        }
    }

    public ResponseEntity<?> getService(ShipReq req) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("shop_id", Integer.parseInt(SHOP_ID));
            body.addProperty("to_district", req.getTo_district_id());
            body.addProperty("from_district", 1451);

            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("v2/shipping-order/available-services",
                    body.toString(), TOKEN, SHOP_ID);
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when get service");
        }
    }

    public HttpResponse<?> create(CreateShipReq req, Order order) {
        try {
            JsonObject body = new JsonObject();
            processAddProperties(body, order, req);
            HttpResponse<?> res = HttpConnectTemplate.connectToGHN("v2/shipping-order/create",
                    body.toString(), TOKEN, SHOP_ID);
            if (res.statusCode() == HttpStatus.OK.value()) {
                return res;
            } else {
                throw new AppException(res.statusCode(), res.body().toString());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Failed when create shipping order");
        }
    }

    private void processAddProperties (JsonObject body, Order order, CreateShipReq req) {
        long weight = order.getTotalProduct()*30L;
        long height = order.getTotalProduct();
        if (weight > 30000) weight = 30000;
        if (height > 150) height = 150;
        JsonArray items = new JsonArray();
        req.getItems().forEach(productItem -> {
            JsonObject object = new JsonObject();
            object.addProperty("name", productItem.getName());
            object.addProperty("quantity", productItem.getQuantity());
            items.add(object);
        });
        body.add("items", items);
        body.addProperty("payment_type_id", 2);
        body.addProperty("service_type_id", (int) order.getShippingDetail().getShipInfo().get("serviceType"));
        body.addProperty("required_note", "Hàng dễ vỡ");
        body.addProperty("content", "Đơn hàng được mua tại ND Store");
        body.addProperty("client_order_code", order.getId());
        body.addProperty("length",100);
        body.addProperty("width",50);
        body.addProperty("height",height);
        body.addProperty("weight",weight);
        body.addProperty("to_name", order.getShippingDetail().getReceiverName());
        body.addProperty("to_phone", order.getShippingDetail().getReceiverPhone());
        body.addProperty("to_address", order.getShippingDetail().getReceiverAddress());
        body.addProperty("cod_amount", order.getTotalPrice().intValue());
        body.addProperty("to_province_name", req.getTo_province_name());
        body.addProperty("to_district_name", req.getTo_district_name());
        body.addProperty("to_ward_name", req.getTo_ward_name());
        body.addProperty("to_ward_code", order.getShippingDetail().getReceiverWard());
    }
}