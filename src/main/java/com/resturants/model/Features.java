package com.resturants.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Features {

    private boolean hasTableBooking;
    private boolean hasOnlineDelivery;
    private boolean isDeliveringNow;
    private boolean switchToOrderMenu;
}
