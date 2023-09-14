package com.example.officepcstore.service;

import com.example.officepcstore.config.CloudinaryConfig;
import com.example.officepcstore.config.Constant;
import com.example.officepcstore.excep.AppException;
import com.example.officepcstore.excep.NotFoundException;
import com.example.officepcstore.map.ProductMap;
import com.example.officepcstore.models.enity.Brand;
import com.example.officepcstore.models.enity.Category;
import com.example.officepcstore.models.enity.product.Product;
import com.example.officepcstore.models.enity.product.ProductImage;
import com.example.officepcstore.payload.ResponseObjectData;
import com.example.officepcstore.payload.request.ProductReq;
import com.example.officepcstore.payload.response.AllProductResponse;
import com.example.officepcstore.payload.response.ProductResponse;
import com.example.officepcstore.repository.BrandRepository;
import com.example.officepcstore.repository.CategoryRepository;
import com.example.officepcstore.repository.ProductRepository;
import com.example.officepcstore.repository.UserRepository;
import com.example.officepcstore.utils.RecommendProductUtils;
import com.example.officepcstore.utils.StringUtils;
import com.mongodb.MongoWriteException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMap productMap;
    private final CloudinaryConfig cloudinary;
//    private final RecommendProductUtils recommendCheckUtils;
//    private final TaskScheduler taskScheduler;
//    private final UserRepository userRepository;
//    public ResponseEntity<?> findAll(String state, Pageable pageable) {
//        Page<Product> products;
//        if (state.equalsIgnoreCase(Constant.ENABLE) || state.equalsIgnoreCase(Constant.DISABLE))
//            products = productRepository.findAllByState(state.toLowerCase(), pageable);
//        else products = productRepository.findAll(pageable);
//        List<AllProductResponse> resList = products.getContent().stream().map(productMap::toGetAllProductRes).collect(Collectors.toList());
//        ResponseEntity<?> resp = getPageProductRes(products, resList);
//        if (resp != null) return resp;
//        throw new NotFoundException("Can not found any product");
//    }

    public ResponseEntity<?> findAllProductByUser( Pageable pageable) {
        Page<Product> products;
            products = productRepository.findAllByState(Constant.ENABLE,pageable);
        List<AllProductResponse> listProduct  = products.getContent().stream().map(productMap::toGetAllProductRes).collect(Collectors.toList());
        ResponseEntity<?> listProductRes = getPageProductRes(products, listProduct);
        if (listProductRes != null)
            return listProductRes;
        throw new NotFoundException("Not found product");
    }

    public ResponseEntity<?> findAllProductByAdmin(String state ,Pageable pageable) {
        Page<Product> products;
        products = productRepository.findAllByState(state,pageable);
        List<AllProductResponse> listProduct  = products.getContent().stream().map(productMap::toGetAllProductRes).collect(Collectors.toList());
        ResponseEntity<?> listProductRes = getPageProductRes(products, listProduct);
        if (listProductRes != null)
            return listProductRes;
        throw new NotFoundException("Not found product");
    }

    public ResponseEntity<?> filterProductPriceByUser(BigDecimal priceMin, BigDecimal priceMax, Pageable pageable ) {
        Long priceMinLong = priceMin.longValue();
        Long priceMaxLong = priceMax.longValue();
        Page<Product> products = productRepository.findAllByPriceBetweenAndState(priceMinLong,priceMaxLong,Constant.ENABLE,pageable);
   List<AllProductResponse> listProduct  = products.getContent().stream().map(productMap::toGetAllProductRes).collect(Collectors.toList());
      ResponseEntity<?> listProductRes = getPageProductRes(products, listProduct);
      if (listProductRes != null)
       return listProductRes;
      throw new NotFoundException("Not found product");
    }


    private ResponseEntity<?> getPageProductRes(Page<Product> products, List<AllProductResponse> resAll) //addPageableToRes
    {
        Map<String, Object> resp = new HashMap<>();
        resp.put("list", resAll);
        resp.put("totalQuantity", products.getTotalElements());
        resp.put("totalPage", products.getTotalPages());
        if (!resAll.isEmpty() )
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObjectData(true, "All product Success ", resp));
        return null;
    }


//    public ResponseEntity<?> findByRecommentUserId(String id, String userId) {
//        Optional<Product> product = productRepository.findProductByIdAndState(id, Constant.ENABLE);
//        if (product.isPresent()) {
//            ProductResponse res = productMap.toGetProductRes(product.get());
//            recommendCheckUtils.setCatId(res.getCategoryId());
//            recommendCheckUtils.setBrandId(res.getBrandId());
//            recommendCheckUtils.setType(Constant.VIEW_TYPE);
//            recommendCheckUtils.setUserId(userId);
//            recommendCheckUtils.setUserRepository(userRepository);
//            taskScheduler.schedule(recommendCheckUtils, new Date(System.currentTimeMillis()));
//            return ResponseEntity.status(HttpStatus.OK).body(
//                    new ResponseObjectData(true, "Get product success", res));
//        }
//        throw new NotFoundException("Can not found any product with id: "+id);
//    }
    public ResponseEntity<?> findById(String id) {
        Optional<Product> product = productRepository.findProductByIdAndState(id, Constant.ENABLE);
        if (product.isPresent()) {
            ProductResponse res = productMap.toGetProductRes(product.get());
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObjectData(true, "Get product Success ", res));
        }
        throw new NotFoundException("Can not found any product");
    }

    public ResponseEntity<?> findByCategoryIdOrBrandId(String id, Pageable pageable) {
        Page<Product> products;
        try {
            Optional<Category> category = categoryRepository.findCategoryByIdAndState(id, Constant.ENABLE);
            if (category.isPresent()) {
                List<ObjectId> subCat = category.get().getSubCategory().stream().map(c -> new ObjectId(c.getId())).collect(Collectors.toList());
                products = productRepository.findProductsByCategory(new ObjectId(id),
                        subCat, pageable);
            } else products = productRepository.findAllByCategory_IdOrBrand_IdAndState(new ObjectId(id),
                    new ObjectId(id),Constant.ENABLE, pageable);
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), "Error when finding");
        }
        List<AllProductResponse> resList = products.stream().map(productMap::toGetAllProductRes).collect(Collectors.toList());
        ResponseEntity<?> resp = getPageProductRes(products, resList);
        if (resp != null) return resp;
        throw new NotFoundException("Can not found any product with category or brand id: "+id);
    }
    public ResponseEntity<?> search(String key, Pageable pageable) {
        Page<Product> products;
        try {
            products = productRepository.findAllBy(TextCriteria
                            .forDefaultLanguage().matchingAny(key),
                    pageable);
        } catch (Exception e) {
            throw new NotFoundException("Can not found any product with: "+key);
        }
        List<AllProductResponse> resList = products.getContent().stream().map(productMap::toGetAllProductRes).collect(Collectors.toList());
        ResponseEntity<?> resp = getPageProductRes(products, resList);
        if (resp != null) return resp;
        throw new NotFoundException("Can not found any product with: "+key);
    }

    public ResponseEntity<?> createProduct(ProductReq req) {
        if (req != null) {
            Product product = productMap.toProductModel(req);
            try {
                productRepository.save(product);
            } catch (Exception e) {
                throw new AppException(HttpStatus.CONFLICT.value(), "Product  already exists");
            }
            ProductResponse res = productMap.toGetProductRes(product);
            product.setReducedPrice(res.getDiscountPrice());
            productRepository.save(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ResponseObjectData(true, "Create product successfully ", res)
            );
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ResponseObjectData(false, "Request is null", "")
        );
    }
    public ResponseEntity<?> updateProduct(String id, ProductReq productReq) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent() && productReq != null) {
            if (!productReq.getName().equals(product.get().getName()))
                product.get().setName(productReq.getName());
            try {
                productRepository.save(product.get());
            } catch (MongoWriteException e) {
                throw new AppException(HttpStatus.CONFLICT.value(), "Product name already exists");
            } catch (Exception e) {
                throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), e.getMessage());
            }
            ProductResponse res = productMap.toGetProductRes(product.get());
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObjectData(true, "Update product successfully ", res)
            );
        }
        throw new NotFoundException("Can not found product with id: "+id);
    }

    public void processUpdate(ProductReq req, Product product) {
        if (!req.getName().equals(product.getName()))
            product.setName(req.getName());
        if (!req.getDescription().equals(product.getDescription()))
            product.setDescription(req.getDescription());
        if (!req.getPrice().equals(product.getPrice()))
            product.setPrice(req.getPrice());
        if (req.getDiscount() != product.getDiscount())
            product.setDiscount(req.getDiscount());
        if (!req.getCategory().equals(product.getCategory().getId())) {
            Optional<Category> category = categoryRepository.findCategoryByIdAndState(req.getCategory(), Constant.ENABLE);
            if (category.isPresent())
                product.setCategory(category.get());
            else throw new NotFoundException("Can not found category with id: "+req.getCategory());
        }
        if (!req.getBrand().equals(product.getBrand().getId())) {
            Optional<Brand> brand = brandRepository.findBrandByIdAndState(req.getBrand(), Constant.ENABLE);
            if (brand.isPresent())
                product.setBrand(brand.get());
            else throw new NotFoundException("Can not found brand with id: "+req.getBrand());
        }
        if (req.getState() != null && !req.getState().isEmpty() &&
                (req.getState().equalsIgnoreCase(Constant.ENABLE) ||
                        req.getState().equalsIgnoreCase(Constant.DISABLE)))
            product.setState(req.getState());
        else throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid state");
    }
    public ResponseEntity<?> createProductConfig(String productId, List<Map<String, String>> mapList) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isPresent()) {
//            Map<String, String> resp = new HashMap<>();
//            resp.put(productConfigurationReq.getNameSetting(), productConfigurationReq.getDescriptionSetting());
           product.get().setProductConfiguration(mapList);
            try {
                productRepository.save(product.get());
            } catch (Exception e) {
                throw new AppException(HttpStatus.CONFLICT.value(), "Product  already exists");
            }
            ProductResponse res = productMap.toGetProductRes(product.get());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ResponseObjectData(true, "Create product successfully ", res)
            );
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ResponseObjectData(false, "Request is null", "")
        );
    }
    @Transactional
    public ResponseEntity<?> addImagesToProduct(String id, List<MultipartFile> files) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            try {
                if (files == null || files.isEmpty() ) throw new AppException(HttpStatus.BAD_REQUEST.value(), "Images is empty");
                files.forEach(f -> {
                    try {
                        String url = cloudinary.uploadImage(f, null);
                        product.get().getImages().add(new ProductImage(UUID.randomUUID().toString(), url));
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        throw new AppException(HttpStatus.EXPECTATION_FAILED.value(), "Error when upload images");
                    }
                    productRepository.save(product.get());
                });
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ResponseObjectData(true, "Add image to product successfully", product.get().getImages())
                );
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new NotFoundException("Error when save image: " + e.getMessage());
            }
        } throw new NotFoundException("Can not found product with id: " + id);
    }

    @Transactional
    public ResponseEntity<?> deleteImageFromProduct(String id, String imageId) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent() && !product.get().getImages().isEmpty()) {
            try {
                Optional<ProductImage> checkDelete = product.get().getImages().stream().filter(i -> i.getId_image().equals(imageId)).findFirst();
                if (checkDelete.isPresent()) {
                    cloudinary.deleteImage(checkDelete.get().getUrl());
                    product.get().getImages().remove(checkDelete.get());
                    productRepository.save(product.get());
                    return ResponseEntity.status(HttpStatus.OK).body(
                            new ResponseObjectData(true, "Delete image successfully", imageId)
                    );
                } else throw new NotFoundException("Can not found image in product with id: " + imageId);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new NotFoundException("Can not found product with id: " + id);
            }
        } throw new NotFoundException("Can not found any image or product with id: " + id);
    }

}
