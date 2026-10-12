package com.example.item.demo35.service;

import com.example.item.demo35.entity.Address;

import java.util.List;

public interface AddressService {
    boolean addAddress(Integer userId, Address address);
    
    List<Address> getAddressList(Integer userId);
    
    Address getAddressDetail(Integer userId, Integer id);
    
    boolean updateAddress(Integer userId, Address address);
    
    boolean deleteAddress(Integer userId, Integer id);
    
    boolean setDefaultAddress(Integer userId, Integer id);
}