package com.assessment.interest_calculator.service;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.assessment.interest_calculator.config.DigioAAProperties;
import com.assessment.interest_calculator.dto.ConsentRequestDTO;
import com.assessment.interest_calculator.dto.ConsentResponseDTO;
import com.assessment.interest_calculator.dto.DigioErrorResponseDTO;
import com.assessment.interest_calculator.exception.DigioAAException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigioAAClient {
    private final WebClient digioWebClient;
    private final DigioAAProperties digioAAProperties;

    /**
     * Makes a consent request to Digio AA. This should notify the customer as well based on the consent request details.
     * @param consentRequest
     * @return ConsentResponseDTO - response from Digio AA
     */
    public Mono<ConsentResponseDTO> makeConsentRequest(ConsentRequestDTO consentRequest) {
        log.info("Making consent request to Digio AA for customerRefId: {}", consentRequest.getCustomerDetails().getCustomerRefId());
        return digioWebClient.post()
                .uri("/fiu_api/client/consent/request/")
                .bodyValue(consentRequest)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(DigioErrorResponseDTO.class)
                        .flatMap(error -> {
                            log.error("Digio AA error - Code: {}, Message: {}, Details: {}",
                                error.getErrorCode(), error.getErrorMsg(), error.getDetails());
                            return Mono.error(new DigioAAException(
                                error.getErrorMsg(),
                                error.getErrorCode(),
                                error.getDetails(),
                                error.getVer()
                            ));
                        })
                )
                .bodyToMono(ConsentResponseDTO.class)
                .doOnSuccess(response -> log.info("Received consent response for customerRefId: {}", consentRequest.getCustomerDetails().getCustomerRefId()))
                .doOnError(error -> log.error("Error while making consent request for customerRefId: {}", consentRequest.getCustomerDetails().getCustomerRefId(), error));
    }
    
}
