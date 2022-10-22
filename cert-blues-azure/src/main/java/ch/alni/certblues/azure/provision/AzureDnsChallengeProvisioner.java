/*
 * MIT License
 *
 * Copyright (c) 2020, 2021 Alexander Nikiforov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package ch.alni.certblues.azure.provision;

import com.azure.resourcemanager.dns.fluent.RecordSetsClient;
import com.azure.resourcemanager.dns.fluent.models.RecordSetInner;
import com.azure.resourcemanager.dns.models.RecordType;
import com.azure.resourcemanager.dns.models.TxtRecord;

import java.util.ArrayList;
import java.util.List;

import ch.alni.certblues.acme.facade.DnsChallengeProvisioner;
import reactor.core.publisher.Mono;

/**
 * Uses Azure services to implement the challenge provisioning for ACME.
 */
public class AzureDnsChallengeProvisioner implements DnsChallengeProvisioner {

    public static final String RECORD_SET_NAME_ACME_CHALLENGE = "_acme-challenge";

    private final AuthenticatedDnsZoneManager dnsZoneManager;
    private final String resourceGroupName;
    private final String dnsZoneName;

    public AzureDnsChallengeProvisioner(AuthenticatedDnsZoneManager dnsZoneManager,
                                        String resourceGroupName, String dnsZoneName) {
        this.dnsZoneManager = dnsZoneManager;
        this.resourceGroupName = resourceGroupName;
        this.dnsZoneName = dnsZoneName;
    }

    @Override
    public Mono<Void> provisionDns(String host, String value) {
        final RecordSetsClient recordSetsClient = dnsZoneManager.getDnsZoneManager()
                .serviceClient()
                .getRecordSets();

        return recordSetsClient.getWithResponseAsync(
                        resourceGroupName,
                        dnsZoneName,
                        getRecordSetName(host),
                        RecordType.TXT
                )
                .flatMap(response -> {
                    final TxtRecord txtRecord = new TxtRecord().withValue(List.of(value));

                    if (response.getValue() == null) {
                        // there is no such record set with this name yet, it should be created
                        return updateRecordSet(recordSetsClient, host, List.of(txtRecord));
                    }
                    else {
                        // add a new record into the existing record set
                        final List<TxtRecord> txtRecords = response.getValue().txtRecords();
                        final int size = txtRecords.size();

                        // leave at most 5 records in the record set
                        final List<TxtRecord> updatedRecordSet = new ArrayList<>();
                        updatedRecordSet.add(txtRecord);
                        updatedRecordSet.addAll(txtRecords.subList(0, Math.min(size, 5)));

                        return updateRecordSet(recordSetsClient, host, updatedRecordSet);
                    }
                });
    }

    private Mono<Void> updateRecordSet(RecordSetsClient recordSetsClient, String host, List<TxtRecord> txtRecords) {
        return recordSetsClient.createOrUpdateAsync(
                        resourceGroupName,
                        dnsZoneName,
                        getRecordSetName(host),
                        RecordType.TXT,
                        new RecordSetInner().withTxtRecords(txtRecords).withTtl(3600L))
                .then();
    }

    private String getRecordSetName(String host) {
        if (dnsZoneName.equalsIgnoreCase(host)) {
            return RECORD_SET_NAME_ACME_CHALLENGE;
        }
        else {
            // i.e. "www.domain.com" will turn into "www" for the DNS zone domain.com
            final String recordName = host.replace("." + dnsZoneName, "");
            return RECORD_SET_NAME_ACME_CHALLENGE + "." + recordName;
        }
    }
}
