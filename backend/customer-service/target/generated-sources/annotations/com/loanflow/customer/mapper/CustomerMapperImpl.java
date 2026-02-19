package com.loanflow.customer.mapper;

import com.loanflow.customer.domain.entity.Address;
import com.loanflow.customer.domain.entity.Customer;
import com.loanflow.customer.domain.enums.Gender;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T15:23:12+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 20.0.1 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public Customer toEntity(CustomerRequest request) {
        if ( request == null ) {
            return null;
        }

        Customer.CustomerBuilder customer = Customer.builder();

        customer.gender( stringToGender( request.getGender() ) );
        customer.currentAddress( toAddress( request.getCurrentAddress() ) );
        customer.permanentAddress( toAddress( request.getPermanentAddress() ) );
        customer.firstName( request.getFirstName() );
        customer.middleName( request.getMiddleName() );
        customer.lastName( request.getLastName() );
        customer.dateOfBirth( request.getDateOfBirth() );
        customer.email( request.getEmail() );
        customer.mobileNumber( request.getMobileNumber() );
        customer.panNumber( request.getPanNumber() );
        customer.aadhaarNumber( request.getAadhaarNumber() );
        customer.segment( request.getSegment() );

        return customer.build();
    }

    @Override
    public CustomerResponse toResponse(Customer entity) {
        if ( entity == null ) {
            return null;
        }

        CustomerResponse.CustomerResponseBuilder customerResponse = CustomerResponse.builder();

        customerResponse.currentAddress( toAddressResponse( entity.getCurrentAddress() ) );
        customerResponse.permanentAddress( toAddressResponse( entity.getPermanentAddress() ) );
        customerResponse.id( entity.getId() );
        customerResponse.customerNumber( entity.getCustomerNumber() );
        customerResponse.firstName( entity.getFirstName() );
        customerResponse.middleName( entity.getMiddleName() );
        customerResponse.lastName( entity.getLastName() );
        customerResponse.dateOfBirth( entity.getDateOfBirth() );
        customerResponse.email( entity.getEmail() );
        customerResponse.mobileNumber( entity.getMobileNumber() );
        customerResponse.aadhaarVerified( entity.getAadhaarVerified() );
        customerResponse.panVerified( entity.getPanVerified() );
        customerResponse.segment( entity.getSegment() );
        customerResponse.createdAt( entity.getCreatedAt() );
        customerResponse.updatedAt( entity.getUpdatedAt() );

        customerResponse.fullName( entity.getFullName() );
        customerResponse.age( entity.getAge() );
        customerResponse.status( entity.getStatus() != null ? entity.getStatus().name() : null );
        customerResponse.kycStatus( entity.getKycStatus() != null ? entity.getKycStatus().name() : null );
        customerResponse.gender( entity.getGender() != null ? entity.getGender().name() : null );
        customerResponse.panNumber( entity.getMaskedPan() );
        customerResponse.aadhaarNumber( entity.getMaskedAadhaar() );

        return customerResponse.build();
    }

    @Override
    public void updateEntity(Customer entity, CustomerRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.getFirstName() != null ) {
            entity.setFirstName( request.getFirstName() );
        }
        if ( request.getMiddleName() != null ) {
            entity.setMiddleName( request.getMiddleName() );
        }
        if ( request.getLastName() != null ) {
            entity.setLastName( request.getLastName() );
        }
        if ( request.getDateOfBirth() != null ) {
            entity.setDateOfBirth( request.getDateOfBirth() );
        }
        if ( request.getGender() != null ) {
            entity.setGender( Enum.valueOf( Gender.class, request.getGender() ) );
        }
        if ( request.getEmail() != null ) {
            entity.setEmail( request.getEmail() );
        }
        if ( request.getMobileNumber() != null ) {
            entity.setMobileNumber( request.getMobileNumber() );
        }
        if ( request.getPanNumber() != null ) {
            entity.setPanNumber( request.getPanNumber() );
        }
        if ( request.getAadhaarNumber() != null ) {
            entity.setAadhaarNumber( request.getAadhaarNumber() );
        }
        if ( request.getSegment() != null ) {
            entity.setSegment( request.getSegment() );
        }
        if ( request.getCurrentAddress() != null ) {
            if ( entity.getCurrentAddress() == null ) {
                entity.setCurrentAddress( Address.builder().build() );
            }
            addressRequestToAddress( request.getCurrentAddress(), entity.getCurrentAddress() );
        }
        if ( request.getPermanentAddress() != null ) {
            if ( entity.getPermanentAddress() == null ) {
                entity.setPermanentAddress( Address.builder().build() );
            }
            addressRequestToAddress( request.getPermanentAddress(), entity.getPermanentAddress() );
        }
    }

    protected void addressRequestToAddress(CustomerRequest.AddressRequest addressRequest, Address mappingTarget) {
        if ( addressRequest == null ) {
            return;
        }

        if ( addressRequest.getAddressLine1() != null ) {
            mappingTarget.setAddressLine1( addressRequest.getAddressLine1() );
        }
        if ( addressRequest.getAddressLine2() != null ) {
            mappingTarget.setAddressLine2( addressRequest.getAddressLine2() );
        }
        if ( addressRequest.getLandmark() != null ) {
            mappingTarget.setLandmark( addressRequest.getLandmark() );
        }
        if ( addressRequest.getCity() != null ) {
            mappingTarget.setCity( addressRequest.getCity() );
        }
        if ( addressRequest.getState() != null ) {
            mappingTarget.setState( addressRequest.getState() );
        }
        if ( addressRequest.getPinCode() != null ) {
            mappingTarget.setPinCode( addressRequest.getPinCode() );
        }
        if ( addressRequest.getCountry() != null ) {
            mappingTarget.setCountry( addressRequest.getCountry() );
        }
        if ( addressRequest.getOwnershipType() != null ) {
            mappingTarget.setOwnershipType( addressRequest.getOwnershipType() );
        }
        if ( addressRequest.getYearsAtAddress() != null ) {
            mappingTarget.setYearsAtAddress( addressRequest.getYearsAtAddress() );
        }
    }
}
