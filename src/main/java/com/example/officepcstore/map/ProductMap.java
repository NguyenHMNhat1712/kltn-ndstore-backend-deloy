package com.example.officepcstore.map;

import com.example.officepcstore.config.Constant;
import com.example.officepcstore.excep.NotFoundException;
import com.example.officepcstore.models.enity.Brand;
import com.example.officepcstore.models.enity.Category;
import com.example.officepcstore.models.enity.product.Product;
import com.example.officepcstore.models.enity.product.ProductImage;
import com.example.officepcstore.payload.request.ProductReq;
import com.example.officepcstore.payload.response.AllProductResponse;
import com.example.officepcstore.payload.response.ProductResponse;
import com.example.officepcstore.repository.BrandRepository;
import com.example.officepcstore.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductMap {
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;


//    public Product toProductModel(ProductReq req) {
//        Optional<Category> category = categoryRepository.findCategoryByIdAndState(req.getCategory(), Constant.ENABLE);
//        Optional<Brand> brand = brandRepository.findBrandByIdAndState(req.getBrand(), Constant.ENABLE);
//        if (category.isEmpty() || brand.isEmpty())
//            throw new NotFoundException(" category or brand not found");
//        return new Product(req.getName(), req.getDescription(), req.getPrice(),req.getDiscount(), req.getStock(),
//                category.get(), brand.get(), Constant.ENABLE);
//    }

    public Product putProductModel(ProductReq req) {
        Optional<Category> category = categoryRepository.findCategoryByIdAndState(req.getCategory(), Constant.ENABLE);
        Optional<Brand> brand = brandRepository.findBrandByIdAndState(req.getBrand(), Constant.ENABLE);
        if (category.isEmpty() || brand.isEmpty())
            throw new NotFoundException(" category or brand not found");
        return new Product(req.getName(), req.getDescription(), req.getPrice(),req.getDiscount(), req.getStock(),
                category.get(), brand.get(),req.getProductConfiguration(),Constant.DISABLE);
    }



    public AllProductResponse toGetAllProductRes(Product req) {
        String discountCalculate = (req.getPrice()).multiply(BigDecimal.valueOf((double) (100- req.getDiscount())/100))
                .stripTrailingZeros().toPlainString();
        BigDecimal discountPrice = new BigDecimal(discountCalculate);
        return new  AllProductResponse(req.getId(), req.getName(), req.getDescription(),
                req.getPrice(),discountPrice, req.getDiscount(), req.getStock(), req.getSold(), req.getRate(),
                req.getNumberProductVote(), req.getCategory().getId(),
                req.getCategory().getTitleCategory(), req.getBrand().getId(),
                req.getBrand().getName(), req.getState(), req.getCreatedDate(),req.getProductImageList(),req.getProductConfiguration());
    }


    public ProductResponse toGetProductRes(Product req) {
        String discountCalculate = req.getPrice().multiply(BigDecimal.valueOf((double) (100- req.getDiscount())/100))
                .stripTrailingZeros().toPlainString();
        BigDecimal discountPrice = new BigDecimal(discountCalculate);
        return new ProductResponse(req.getId(), req.getName(), req.getDescription(),
                req.getPrice(),discountPrice,req.getDiscount(),req.getStock(), req.getSold(), req.getRate(), req.getNumberProductVote(),
                req.getCategory().getId(), req.getCategory().getTitleCategory(),req.getBrand().getId(),
                req.getBrand().getName(), req.getState(), req.getCreatedDate(),req.getProductImageList(),req.getProductConfiguration());
    }
}
