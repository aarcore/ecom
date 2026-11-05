package com.ait.mall.core.service.impl;

import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.mall.core.entity.MemberAddress;
import com.ait.mall.core.mapper.MemberAddressMapper;
import com.ait.mall.core.service.AddressService;
import com.ait.mall.core.vo.AddressVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final MemberAddressMapper addressMapper;

    @Override
    public List<AddressVo> list(Long memberId) {
        return addressMapper.selectList(Wrappers.<MemberAddress>lambdaQuery()
                        .eq(MemberAddress::getMemberId, memberId)
                        .orderByDesc(MemberAddress::getIsDefault)
                        .orderByDesc(MemberAddress::getId))
                .stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVo save(Long memberId, AddressVo request) {
        MemberAddress entity;
        if (request.getId() != null) {
            entity = addressMapper.selectById(request.getId());
            if (entity == null || !memberId.equals(entity.getMemberId())) {
                throw new BizException(ResultCode.ADDRESS_NOT_FOUND);
            }
        } else {
            entity = new MemberAddress();
            entity.setMemberId(memberId);
        }

        entity.setReceiver(request.getReceiver());
        entity.setMobile(request.getMobile());
        entity.setProvince(request.getProvince());
        entity.setCity(request.getCity());
        entity.setDistrict(request.getDistrict());
        entity.setDetail(request.getDetail());
        entity.setPostalCode(request.getPostalCode());

        boolean wantsDefault = Integer.valueOf(1).equals(request.getIsDefault());
        Long existing = addressMapper.selectCount(Wrappers.<MemberAddress>lambdaQuery()
                .eq(MemberAddress::getMemberId, memberId));
        if (existing == null || existing == 0) {
            wantsDefault = true;
        }
        entity.setIsDefault(wantsDefault ? 1 : 0);

        if (entity.getId() == null) {
            addressMapper.insert(entity);
        } else {
            addressMapper.updateById(entity);
        }

        if (wantsDefault) {
            clearOtherDefaults(memberId, entity.getId());
        }
        return toVo(entity);
    }

    @Override
    public void delete(Long memberId, Long addressId) {
        MemberAddress entity = addressMapper.selectById(addressId);
        if (entity == null || !memberId.equals(entity.getMemberId())) {
            throw new BizException(ResultCode.ADDRESS_NOT_FOUND);
        }
        addressMapper.deleteById(addressId);
        // promote another address to default so checkout always has a target
        MemberAddress next = addressMapper.selectOne(Wrappers.<MemberAddress>lambdaQuery()
                .eq(MemberAddress::getMemberId, memberId)
                .orderByDesc(MemberAddress::getId)
                .last("limit 1"));
        if (next != null) {
            next.setIsDefault(1);
            addressMapper.updateById(next);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long addressId) {
        MemberAddress entity = addressMapper.selectById(addressId);
        if (entity == null || !memberId.equals(entity.getMemberId())) {
            throw new BizException(ResultCode.ADDRESS_NOT_FOUND);
        }
        entity.setIsDefault(1);
        addressMapper.updateById(entity);
        clearOtherDefaults(memberId, addressId);
    }

    @Override
    public AddressVo get(Long memberId, Long addressId) {
        MemberAddress entity = addressMapper.selectById(addressId);
        if (entity == null || !memberId.equals(entity.getMemberId())) {
            throw new BizException(ResultCode.ADDRESS_NOT_FOUND);
        }
        return toVo(entity);
    }

    @Override
    public AddressVo resolveForCheckout(Long memberId, Long addressId) {
        if (addressId != null) {
            MemberAddress entity = addressMapper.selectById(addressId);
            if (entity != null && memberId.equals(entity.getMemberId())) {
                return toVo(entity);
            }
        }
        List<MemberAddress> candidates = addressMapper.selectList(Wrappers.<MemberAddress>lambdaQuery()
                .eq(MemberAddress::getMemberId, memberId)
                .orderByDesc(MemberAddress::getIsDefault)
                .orderByDesc(MemberAddress::getId));
        return candidates.isEmpty() ? null : toVo(candidates.get(0));
    }

    private void clearOtherDefaults(Long memberId, Long keepId) {
        MemberAddress patch = new MemberAddress();
        patch.setIsDefault(0);
        addressMapper.update(patch, Wrappers.<MemberAddress>lambdaUpdate()
                .eq(MemberAddress::getMemberId, memberId)
                .ne(MemberAddress::getId, keepId));
    }

    private AddressVo toVo(MemberAddress entity) {
        AddressVo vo = new AddressVo();
        vo.setId(entity.getId());
        vo.setReceiver(entity.getReceiver());
        vo.setMobile(entity.getMobile());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetail(entity.getDetail());
        vo.setPostalCode(entity.getPostalCode());
        vo.setIsDefault(entity.getIsDefault());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setFullAddress(buildFull(entity));
        return vo;
    }

    static String buildFull(MemberAddress entity) {
        StringBuilder sb = new StringBuilder();
        append(sb, entity.getProvince());
        append(sb, entity.getCity());
        append(sb, entity.getDistrict());
        append(sb, entity.getDetail());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part != null && !part.isEmpty()) {
            sb.append(part);
        }
    }
}
