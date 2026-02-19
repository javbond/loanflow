package com.loanflow.customer.mapper;

import com.loanflow.customer.domain.entity.Address;
import com.loanflow.customer.domain.entity.Customer;
import com.loanflow.customer.domain.enums.Gender;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "kycStatus", ignore = true)
    @Mapping(target = "aadhaarVerified", ignore = true)
    @Mapping(target = "panVerified", ignore = true)
    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    @Mapping(target = "currentAddress", source = "currentAddress", qualifiedByName = "toAddress")
    @Mapping(target = "permanentAddress", source = "permanentAddress", qualifiedByName = "toAddress")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "fullName", expression = "java(entity.getFullName())")
    @Mapping(target = "age", expression = "java(entity.getAge())")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    @Mapping(target = "kycStatus", expression = "java(entity.getKycStatus() != null ? entity.getKycStatus().name() : null)")
    @Mapping(target = "gender", expression = "java(entity.getGender() != null ? entity.getGender().name() : null)")
    @Mapping(target = "panNumber", expression = "java(entity.getMaskedPan())")
    @Mapping(target = "aadhaarNumber", expression = "java(entity.getMaskedAadhaar())")
    @Mapping(target = "currentAddress", source = "currentAddress", qualifiedByName = "toAddressResponse")
    @Mapping(target = "permanentAddress", source = "permanentAddress", qualifiedByName = "toAddressResponse")
    @Mapping(target = "activeLoansCount", ignore = true)
    @Mapping(target = "totalOutstanding", ignore = true)
    CustomerResponse toResponse(Customer entity);

    @Named("stringToGender")
    default Gender stringToGender(String gender) {
        if (gender == null) {
            return null;
        }
        return Gender.valueOf(gender.toUpperCase());
    }

    @Named("toAddress")
    default Address toAddress(CustomerRequest.AddressRequest addressRequest) {
        if (addressRequest == null) {
            return null;
        }
        return Address.builder()
                .addressLine1(addressRequest.getAddressLine1())
                .addressLine2(addressRequest.getAddressLine2())
                .landmark(addressRequest.getLandmark())
                .city(addressRequest.getCity())
                .state(addressRequest.getState())
                .pinCode(addressRequest.getPinCode())
                .country(addressRequest.getCountry())
                .ownershipType(addressRequest.getOwnershipType())
                .yearsAtAddress(addressRequest.getYearsAtAddress())
                .build();
    }

    @Named("toAddressResponse")
    default CustomerResponse.AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return CustomerResponse.AddressResponse.builder()
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .pinCode(address.getPinCode())
                .country(address.getCountry())
                .ownershipType(address.getOwnershipType())
                .yearsAtAddress(address.getYearsAtAddress())
                .fullAddress(address.getFullAddress())
                .build();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Customer entity, CustomerRequest request);
}
