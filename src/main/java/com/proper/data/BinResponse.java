package com.proper.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Lebel on 17/02/2015.
 */
public class BinResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String RequestedBinCode;
    private int MatchedProducts;
    private List<ProductBinResponse> Products;

    public BinResponse() {
    }

    public BinResponse(String requestedBinCode, int matchedProducts, List<ProductBinResponse> products) {
        RequestedBinCode = requestedBinCode;
        MatchedProducts = matchedProducts;
        Products = products;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @JsonProperty("RequestedBinCode")
    public String getRequestedBinCode() {
        return RequestedBinCode;
    }

    public void setRequestedBinCode(String requestedBinCode) {
        RequestedBinCode = requestedBinCode;
    }

    @JsonProperty("MatchedProducts")
    public int getMatchedProducts() {
        return MatchedProducts;
    }

    public void setMatchedProducts(int matchedProducts) {
        MatchedProducts = matchedProducts;
    }

    @JsonProperty("Products")
    public List<ProductBinResponse> getProducts() {
        return Products;
    }

    public void setProducts(List<ProductBinResponse> products) {
        Products = products;
    }
}

