package com.sap.mrm.sample.Upload;

import java.io.StringReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Controller
public class SampleController {

	@RequestMapping(value = "/uploadECBdata", method = RequestMethod.GET, produces = "text/plain")
	ResponseEntity<String> uploadECBData() {

		// Step 1: Fetch rates from your market data provider - ECB in this case - via
		// their APIs

		ResponseEntity<String> ecbResponse = fetchRatesFromECB();

		if (ecbResponse == null || !ecbResponse.getStatusCode().is2xxSuccessful() || !ecbResponse.hasBody()) {
			return new ResponseEntity<>("Rates not available from Data Provider ", HttpStatus.NO_CONTENT);
		}

		// Step 2: Parse rates from your market data provider and convert them into the
		// MRM Upload CSV or JSON format

		List<MarketDataUploadFormat> marketDataUploadList = parseECBRatesToUploadFormat(ecbResponse);

		if (marketDataUploadList == null || marketDataUploadList.isEmpty()) {
			return new ResponseEntity<>("Rates from Data Provider could not be parsed successfully",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Step 3: Upload the parsed rates to the MRM service

		ResponseEntity<String> mrmResponse = uploadMarketDataToMRM(marketDataUploadList);

		return mrmResponse;
	}

	private ResponseEntity<String> uploadMarketDataToMRM(List<MarketDataUploadFormat> marketDataUploadList) {

		// Step 1: Get URLs and Credentials to access the MRM service

		String uploadUrl = null, authUrl = null, clientId = null, clientSecret = null;
		try {
			String vcapServices = System.getenv("VCAP_SERVICES");
			if (vcapServices == null || vcapServices.isEmpty())
				return null;
			JsonReader vcapReader = Json.createReader(new StringReader(vcapServices));
			JsonObject credentials = vcapReader.readObject().getJsonArray("market-rates-byor").getJsonObject(0)
					.getJsonObject("credentials");
			uploadUrl = credentials.getString("uploadUrl");
			JsonObject credUaa = credentials.getJsonObject("uaa");
			authUrl = credUaa.getString("url");
			clientId = credUaa.getString("clientid");
			clientSecret = credUaa.getString("clientsecret");
		} catch (Exception e) {
			System.out.println("Could not access MRM instance details - " + e.getMessage());
			return null;
		}

		// Step 2: Generate auth token to access MRM service

		String authToken = generateAuthToken(authUrl, clientId, clientSecret);

		// Step 3: Post rates to MRM using the generated auth token

		RestTemplate restTemplate = getRestTemplate();
		ObjectMapper objectMapper = new ObjectMapper();
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		requestHeaders.add("Authorization", "Bearer " + authToken);
		try {
			HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(marketDataUploadList),
					requestHeaders);
			ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
					String.class);
			return uploadResponse;
		} catch (RestClientException e) {
			System.out.println("Error fetching rates from MRM - " + e.getMessage());
		} catch (JsonProcessingException e) {
			System.out.println("Error forming json payload - " + e.getMessage());
		}

		return null;
	}

	String generateAuthToken(String authUrl, String clientId, String clientSecret) {
		String token = null;
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		HttpHeaders requestHeaders = new HttpHeaders();

		RestTemplate restTemplate = getRestTemplate();

		formData.add("client_id", clientId);
		formData.add("client_secret", clientSecret);
		formData.add("grant_type", "client_credentials");
		formData.add("response_type", "token");

		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		requestHeaders.set("Accept", "*/*");
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, requestHeaders);
		JsonReader authJsonReader = null;
		try {
			ResponseEntity<String> authResponse = restTemplate.exchange(authUrl + "/oauth/token", HttpMethod.POST,
					requestEntity, String.class);
			System.out.println("AuthResponse status - " + authResponse.getStatusCodeValue());
			if (authResponse.getStatusCodeValue() == 200 && authResponse.hasBody()) {
				String authString = authResponse.getBody();
				authJsonReader = Json.createReader(new StringReader(authString));
				JsonObject authJson = authJsonReader.readObject();
				if (!authJson.isNull("access_token") && !authJson.isNull("expires_in")) {
					token = authJson.getString("access_token");
				}
			}
		} catch (RestClientException e) {
			System.out.println("Error fetching auth token - " + e.getMessage());
		} catch (JsonException e) {
			System.out.println("Error fetching auth token - " + e.getMessage());
		} finally {
			if (authJsonReader != null) {
				authJsonReader.close();
			}
			requestHeaders.clear();
		}
		return token;
	}

	private List<MarketDataUploadFormat> parseECBRatesToUploadFormat(ResponseEntity<String> ecbResponse) {

		List<MarketDataUploadFormat> rates = new ArrayList<>();
		String[] lines = ecbResponse.getBody().toString().split("\n");
		String[] properties;

		for (int i = 1; i < lines.length; ++i) {

			// KEY,FREQ,CURRENCY,CURRENCY_DENOM,EXR_TYPE,EXR_SUFFIX,TIME_PERIOD,OBS_VALUE -
			// Received format of data from ECB

			MarketDataUploadFormat rate = new MarketDataUploadFormat();

			properties = lines[i].split(",");

			rate.setProviderCode("ECB"); // The market data provider you are fetching rates from
			rate.setMarketDataSource("ECB"); // The market data source you are fetching rates from
			rate.setMarketDataCategory("01"); // The market data category you are uploading data for - 01 here for
												// Currency Exchange Rates
			rate.setKey2(properties[2]); // to-currency for Currency Exchange Rates
			rate.setKey1(properties[3]); // from-currency for Currency Exchange Rates
			rate.setMarketDataProperty("CLOSE"); // The market data property you are uploading data for - For
													// example, CLOSE here for EOD rates
			rate.setEffectiveDate(properties[6]); // The effective date for the market data
			rate.setEffectiveTime("16:00:00"); // The effective time for the market data
			if (properties[7] == null || properties[7].isEmpty() || properties[7].trim().length() == 0)
				continue;
			try {
				rate.setMarketDataValue(NumberFormat.getInstance().parse(properties[7])); // The market data value
			} catch (ParseException e) {
				System.out.println("Error parsing market data value from provider");
				continue;
			}
			rates.add(rate);

		}
		return rates;
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<String> fetchRatesFromECB() {

		// API URL from the data provider
		String url = "http://sdw-wsrest.ecb.europa.eu/service/data/EXR/D..EUR.SP00.A?detail=dataonly&startPeriod=";

		ResponseEntity<String> response;
		Map<String, String> params = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", "text/csv");
		@SuppressWarnings({ "rawtypes" })
		HttpEntity entity = new HttpEntity(headers);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		url = url + sdf.format(cal.getTime());

		RestTemplate restTemplate = getRestTemplate();

		try {
			response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class, params);
		} catch (RestClientException e) {
			return new ResponseEntity<>("Rates could not be fetched from Data Provider ",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	RestTemplate getRestTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

		RestTemplate restTemplate = new RestTemplate(requestFactory);
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new FormHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());
		messageConverters.add(new MappingJackson2HttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);

		return restTemplate;
	}
}