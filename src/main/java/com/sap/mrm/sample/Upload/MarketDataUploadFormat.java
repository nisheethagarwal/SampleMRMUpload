package com.sap.mrm.sample.Upload;

public class MarketDataUploadFormat {
	
	private String providerCode;
	
	private String marketDataSource;

	private String marketDataCategory;

	private String key1;

	private String key2;

	private String marketDataProperty;

	private String effectiveDate;

	private String effectiveTime;

	private Number marketDataValue;

	private String securityCurrency;

	private Float fromFactor;

	private Float toFactor;

	private String priceQuotation;

	private String additionalKey;
	
	public String getProviderCode() {
		return providerCode;
	}

	public void setProviderCode(String providerCode) {
		this.providerCode = providerCode;
	}

	public String getMarketDataSource() {
		return marketDataSource;
	}

	public void setMarketDataSource(String marketDataSource) {
		this.marketDataSource = marketDataSource;
	}

	public String getMarketDataCategory() {
		return marketDataCategory;
	}

	public void setMarketDataCategory(String marketDataCategory) {
		this.marketDataCategory = marketDataCategory;
	}

	public String getKey1() {
		return key1;
	}

	public void setKey1(String key1) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	public void setKey2(String key2) {
		this.key2 = key2;
	}

	public String getMarketDataProperty() {
		return marketDataProperty;
	}

	public void setMarketDataProperty(String marketDataProperty) {
		this.marketDataProperty = marketDataProperty;
	}

	public String getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(String effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public Number getMarketDataValue() {
		return marketDataValue;
	}

	public void setMarketDataValue(Number marketDataValue) {
		this.marketDataValue = marketDataValue;
	}

	public String getSecurityCurrency() {
		return securityCurrency;
	}

	public void setSecurityCurrency(String securityCurrency) {
		this.securityCurrency = securityCurrency;
	}

	public Float getFromFactor() {
		return fromFactor;
	}

	public void setFromFactor(Float fromFactor) {
		this.fromFactor = fromFactor;
	}

	public Float getToFactor() {
		return toFactor;
	}

	public void setToFactor(Float toFactor) {
		this.toFactor = toFactor;
	}

	public String getPriceQuotation() {
		return priceQuotation;
	}

	public void setPriceQuotation(String priceQuotation) {
		this.priceQuotation = priceQuotation;
	}

	public String getAdditionalKey() {
		return additionalKey;
	}

	public void setAdditionalKey(String additionalKey) {
		this.additionalKey = additionalKey;
	}

}
