package com.lbb.lmps.repository;

import com.lbb.lmps.entity.LmpsTxnDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class LmpsTxnDetailRepositoryTest {

    @Autowired
    private LmpsTxnDetailRepository repository;

    @Test
    void testSaveAndGetLmpsTxnDetailRecords() {
        // Create and save Sample Record 1
        LmpsTxnDetail record1 = createSampleRecord("TXN-SAMPLE-991", "NONCE-SAMPLE-991", "CUST-999");
        LmpsTxnDetail saved1 = repository.save(record1);
        assertThat(saved1.getId()).isNotNull();

        // Create and save Sample Record 2
        LmpsTxnDetail record2 = createSampleRecord("TXN-SAMPLE-992", "NONCE-SAMPLE-992", "CUST-999");
        LmpsTxnDetail saved2 = repository.save(record2);
        assertThat(saved2.getId()).isNotNull();

        // Fetch Record 1 and verify data matches perfectly
        Optional<LmpsTxnDetail> fetched1 = repository.findByTransactionId("TXN-SAMPLE-991");
        assertThat(fetched1).isPresent();
        assertThat(fetched1.get().getCustomerId()).isEqualTo("CUST-999");
        assertThat(fetched1.get().getNonce()).isEqualTo("NONCE-SAMPLE-991");
        assertThat(fetched1.get().getAmount()).isEqualByComparingTo(BigDecimal.TEN);

        // Fetch Record 2 and verify data matches perfectly
        Optional<LmpsTxnDetail> fetched2 = repository.findByTransactionId("TXN-SAMPLE-992");
        assertThat(fetched2).isPresent();
        assertThat(fetched2.get().getCustomerId()).isEqualTo("CUST-999");
        assertThat(fetched2.get().getNonce()).isEqualTo("NONCE-SAMPLE-992");
        assertThat(fetched2.get().getAmount()).isEqualByComparingTo(BigDecimal.TEN);
    }

    private LmpsTxnDetail createSampleRecord(String transactionId, String nonce, String customerId) {
        LmpsTxnDetail record = new LmpsTxnDetail();
        record.setPaymentChannelId(25L);
        record.setCustomerId(customerId);
        record.setTransactionId(transactionId);
        record.setNonce(nonce);
        record.setProviderCode("LMPS");
        record.setStatus("DEBIT_PENDING");
        record.setDrAccountNo("DR-ACCT-999");
        record.setDrCif(customerId);
        record.setCrAccountNo("CR-ACCT-999");
        record.setDrAccountName("Sample Sender");
        record.setCrAccountName("Sample Receiver");
        record.setAmount(BigDecimal.TEN);
        record.setFeeAmt(BigDecimal.ZERO);
        record.setFeeProviderAmt(BigDecimal.ZERO);
        record.setCurrencyCode("LAK");
        record.setFeeCurrencyCode("LAK");
        record.setFeeProviderCurrencyCode("LAK");
        record.setCreatedAt(LocalDateTime.now());
        record.setRemark("TEST RECORD");
        return record;
    }
}
