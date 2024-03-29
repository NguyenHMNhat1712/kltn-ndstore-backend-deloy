package com.example.officepcstore.repository;

import com.example.officepcstore.models.enity.Order;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findOrderByUser_Id(ObjectId userId);

   Optional<Order> findOrderByUser_IdAndStatusOrder(ObjectId userId, String state);
//  Optional<Order> findOrderByIdAndUser_Id(String orderId, ObjectId userId);
    Optional<Order> findOrderByPaymentOrderMethod_PaymentTokenAndStatusOrder(String token, String state);
   Optional<Order> findOrderByIdAndStatusOrder(String orderId, String state);
    Page<Order> findAllByStatusOrder(String state, Pageable pageable);
 // Page<Order> findOrderByUser_Id(ObjectId userId, Pageable pageable);
  //Page<Order> findAllByUser_Id(ObjectId userName, Pageable pageable);
 Page<Order> findAllByShipment_CustomerName(String cusName, Pageable pageable);

    Page<Order> findAllByStatusOrderAndShipment_CustomerNameAndInvoiceDateBetween(String status,String user,LocalDateTime start,LocalDateTime end,Pageable pageable);
  Page<Order> findAllByStatusOrderAndUser_IdAndInvoiceDateBetween(String status,ObjectId user,LocalDateTime start,LocalDateTime end,Pageable pageable);
    Page<Order> findAllByUser_IdAndInvoiceDateBetween(ObjectId userName, LocalDateTime start,LocalDateTime end,Pageable pageable);
    Page<Order> findAllByShipment_CustomerNameLikeIgnoreCaseAndInvoiceDateBetweenAndStatusOrderNot(String userName, LocalDateTime start,LocalDateTime end,String status,Pageable pageable);
//  Page<Order> findAllByPaymentOrderMethod(String paymentMethod, Pageable pageable);

//
//    Page<Order> findAllByPaymentOrderMethodAndStatusOrder(String paymentMethod,String status, Pageable pageable);
  Page<Order> findAllByStatusOrderAndUser_Id(String status,ObjectId userName, Pageable pageable);
    Page<Order> findAllByStatusOrderAndShipment_CustomerNameLikeIgnoreCase(String status,String userName, Pageable pageable);
//    Page<Order> findAllByPaymentOrderMethodAndUser_NameLike(String paymentMethod,String userName, Pageable pageable);
//    Page<Order>findAllByPaymentOrderMethodAndStatusOrderAndUser_NameLike(String payment, String status,String user, Pageable pageable);
//    Page<Order> findAllByPaymentOrderMethodAndStatusOrderAndUser_NameLikeAndInvoiceDateBetween(String payment, String status,String user, LocalDateTime  start,LocalDateTime  end,Pageable pageable);
//
//    Page<Order>findAllByPaymentOrderMethodAndInvoiceDateBetween(String payment, LocalDateTime start,LocalDateTime end,Pageable pageable);
    Page<Order> findOrdersByUser_IdAndStatusOrderNot(ObjectId userId,String state ,Pageable pageable);

    Page<Order> findOrdersByShipment_CustomerNameLikeIgnoreCaseAndStatusOrderNot(String cusName ,String state ,Pageable pageable);
    Page<Order> findOrdersByUser_IdAndStatusOrder(ObjectId userId,String state ,Pageable pageable);

  Page<Order> findAllByInvoiceDateBetweenAndStatusOrder(LocalDateTime from, LocalDateTime to, String state, Pageable pageable);
//    @Aggregation("{ $group: { _id : $state, count: { $sum: 1 } } }")
//    List<StateCountAggregate> countAllByState();
Page<Order> findAllByInvoiceDateBetweenAndStatusOrderNot(LocalDateTime from, LocalDateTime to, String status,Pageable pageable);
    @Query(value=" {statusOrder: {'$nin': ['cart']}}")
    Page<Order> findAllByStatusOrderNoCart( Pageable pageable);
  //  Page<Order>findOrdersByInvoiceDateBetween();
  Page<Order> findAllByStatusOrderNot( String status,Pageable pageable);


    Page<Order> countAllByLastUpdateStateDateBetweenAndStatusOrderOrderByLastUpdateStateDateAsc(LocalDateTime from, LocalDateTime to, String state, Pageable pageable);
}
