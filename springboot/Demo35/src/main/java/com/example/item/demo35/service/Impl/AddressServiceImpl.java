package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.AddressDao;
import com.example.item.demo35.entity.Address;
import com.example.item.demo35.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressDao addressDao;

    @Override
    public boolean addAddress(Integer userId, Address address) {
        address.setUserId(userId);
        if (address.getIsDefault() == 1) {
            addressDao.setDefault(userId, 0);
        }
        return addressDao.insert(address);
    }

    @Override
    public List<Address> getAddressList(Integer userId) {
        return addressDao.selectByUserId(userId);
    }

    @Override
    public Address getAddressDetail(Integer userId, Integer id) {
        Address address = addressDao.selectById(id);
        if (address != null && address.getUserId().equals(userId)) {
            return address;
        }
        return null;
    }

    @Override
    public boolean updateAddress(Integer userId, Address address) {
        Address existing = addressDao.selectById(address.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            return false;
        }
        if (address.getIsDefault() == 1) {
            addressDao.setDefault(userId, 0);
        }
        return addressDao.update(address);
    }

    @Override
    public boolean deleteAddress(Integer userId, Integer id) {
        Address address = addressDao.selectById(id);
        if (address != null && address.getUserId().equals(userId)) {
            return addressDao.delete(id);
        }
        return false;
    }

    @Override
    public boolean setDefaultAddress(Integer userId, Integer id) {
        Address address = addressDao.selectById(id);
        if (address != null && address.getUserId().equals(userId)) {
            return addressDao.setDefault(userId, id);
        }
        return false;
    }
}