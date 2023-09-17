package com.validationsample.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SampleFormTest {

    /**
     * バリデーションのテスト用に使うサンプルフォーム
     */
    public static class SampleForm {

        /**
         * 名前
         * 1文字から20文字まで
         */
        @Size(min = 1, max = 20)
        private String name;

        /**
         * 年齢
         * 0から150まで
         */
        @Min(0)
        @Max(150)
        private Integer age;

        /**
         * 郵便番号
         * 123-4567の形式
         */
        @Pattern(regexp = "^\\d{3}-\\d{4}$", message = "有効な郵便番号を入力してください。 (例: 123-4567)")
        private String postalCode;

        /**
         * カラー
         * RED, BLUE, GREENのいずれか
         */
        @ValidColor
        private String color;

        public SampleForm(String name, Integer age, String postalCode, String color) {
            this.name = name;
            this.age = age;
            this.postalCode = postalCode;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * カラーのEnum
     */
    public enum Color {
        RED,
        BLUE,
        GREEN;

        public static Color from(String value) {
            return Optional.of(Color.valueOf(value.toUpperCase())).orElseThrow(() -> new IllegalArgumentException("有効なカラーを指定してください（RED, BLUE, GREENのいずれか）。"));
        }
    }

    /**
     * カラーのバリデーション用アノテーション
     */
    @Documented
    @Constraint(validatedBy = ColorValidator.class)
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValidColor {
        String message() default "有効なカラーを指定してください（RED, BLUE, GREENのいずれか）。";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Colorのバリデータ
     */
    public static class ColorValidator implements ConstraintValidator<ValidColor, String> {

        @Override
        public void initialize(ValidColor constraintAnnotation) {
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true; // nullの場合はバリデーションしない
            }

            try {
                Color.from(value);
                return true;
            } catch (IllegalArgumentException e) { // 有効なカラーでない場合はバリデーションエラー
                return false;
            }
        }
    }

    private static Validator validator;

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void 名前が1文字未満である時にバリデーションエラーとなること() {
        SampleForm form = new SampleForm("", 20, "123-4567", "RED");
        var violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(tuple("name", "1 から 20 の間のサイズにしてください"));
    }

    @Test
    public void 名前が1文字である時にバリデーションエラーとならないこと() {
        SampleForm form = new SampleForm("a", 20, "123-4567", "RED");
        var violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    public void 名前が20文字である時にバリデーションエラーとならないこと() {
        SampleForm form = new SampleForm("a".repeat(20), 20, "123-4567", "RED");
        var violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    public void 名前が21文字である時にバリデーションエラーとならないこと() {
        SampleForm form = new SampleForm("a".repeat(21), 20, "123-4567", "RED");
        var violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(tuple("name", "1 から 20 の間のサイズにしてください"));
    }

    @Test
    public void 色にREDを指定した場合はバリデーションエラーにならないこと() {
        SampleForm form = new SampleForm("name", 20, "123-4567", "RED");

        var violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    public void 色にORANGEを指定した場合はバリデーションエラーにならないこと() {
        SampleForm form = new SampleForm("name", 20, "123-4567", "ORANGE");

        var violations = validator.validate(form);
        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(tuple("color", "有効なカラーを指定してください（RED, BLUE, GREENのいずれか）。"));
    }

}
