package com.severus.finance.settings;

import com.severus.finance.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "monthly_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "salary_credit_day", nullable = false)
    private int salaryCreditDay;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "theme_preference", nullable = false, length = 20)
    private String themePreference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AppSettings() {
    }

    public AppSettings(UUID id, User user, BigDecimal monthlySalary, int salaryCreditDay, String currency, String timezone, String displayName, String themePreference) {
        this.id = id;
        this.user = user;
        this.monthlySalary = monthlySalary;
        this.salaryCreditDay = salaryCreditDay;
        this.currency = currency;
        this.timezone = timezone;
        this.displayName = displayName;
        this.themePreference = themePreference;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public int getSalaryCreditDay() {
        return salaryCreditDay;
    }

    public void setSalaryCreditDay(int salaryCreditDay) {
        this.salaryCreditDay = salaryCreditDay;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
