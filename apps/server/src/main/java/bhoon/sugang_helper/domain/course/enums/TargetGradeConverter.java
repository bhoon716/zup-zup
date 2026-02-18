package bhoon.sugang_helper.domain.course.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TargetGradeConverter implements AttributeConverter<TargetGrade, String> {

    @Override
    public String convertToDatabaseColumn(TargetGrade attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public TargetGrade convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return TargetGrade.from(dbData);
    }
}
