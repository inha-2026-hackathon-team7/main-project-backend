package com.hackathonteam7.mainprojectbackend.reward;

import com.hackathonteam7.mainprojectbackend.common.persistence.LowerCaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RewardKindConverter extends LowerCaseEnumConverter<RewardKind> {

    public RewardKindConverter() {
        super(RewardKind.class);
    }
}
