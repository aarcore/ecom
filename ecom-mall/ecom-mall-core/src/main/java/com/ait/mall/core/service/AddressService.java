package com.ait.mall.core.service;

import com.ait.mall.core.vo.AddressVo;

import java.util.List;

public interface AddressService {

    List<AddressVo> list(Long memberId);

    AddressVo save(Long memberId, AddressVo request);

    void delete(Long memberId, Long addressId);

    void setDefault(Long memberId, Long addressId);

    AddressVo get(Long memberId, Long addressId);

    /** Returns the requested address, the default one, or the newest one. */
    AddressVo resolveForCheckout(Long memberId, Long addressId);
}
