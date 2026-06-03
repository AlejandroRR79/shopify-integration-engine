package com.creditienda.service.skydropx.dao.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.creditienda.service.skydropx.constants.SkyDropXProcessStatus;
import com.creditienda.service.skydropx.constants.SkyDropXProcessStep;
import com.creditienda.service.skydropx.dao.SkyDropXProcessDAO;
import com.creditienda.service.skydropx.model.SkyDropXProcessRecord;

@Repository
public class SkyDropXProcessDAOImpl implements SkyDropXProcessDAO {

        private static final Logger log = LoggerFactory.getLogger(
                        SkyDropXProcessDAOImpl.class);

        private final NamedParameterJdbcTemplate jdbcTemplate;

        public SkyDropXProcessDAOImpl(
                        NamedParameterJdbcTemplate jdbcTemplate) {

                this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public Long createProcess(
                        SkyDropXProcessRecord processRecord) {

                String sql = "MERGE SKYDROPX_PROCESS WITH (HOLDLOCK) AS target " +
                                "USING ( " +
                                "  SELECT so.idShopifyOrder " +
                                "  FROM SHOPIFY_ORDER so " +
                                "  WHERE so.orderNumber = :orderNumber " +
                                ") AS source " +
                                "ON target.quotationId = :quotationId " +
                                "WHEN MATCHED THEN " +
                                "  UPDATE SET " +
                                "    target.requestJson      = :requestJson, " +
                                "    target.statusId         = :statusId, " +
                                "    target.processStep      = :processStep, " +
                                "    target.retryCount       = :retryCount, " +
                                "    target.isRetryable      = :isRetryable, " +
                                "    target.isActive         = :isActive, " +
                                "    target.hasFinalShipment = :hasFinalShipment, " +
                                "    target.updatedAt        = GETDATE() " +
                                "WHEN NOT MATCHED THEN " +
                                "  INSERT ( " +
                                "    idShopifyOrder, quotationId, requestJson, " +
                                "    statusId, processStep, retryCount, " +
                                "    isRetryable, isActive, hasFinalShipment, " +
                                "    createdAt, updatedAt " +
                                "  ) VALUES ( " +
                                "    source.idShopifyOrder, :quotationId, :requestJson, " +
                                "    :statusId, :processStep, :retryCount, " +
                                "    :isRetryable, :isActive, :hasFinalShipment, " +
                                "    GETDATE(), GETDATE() " +
                                "  );";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "orderNumber",
                                processRecord.getOrderNumber());

                params.addValue(
                                "quotationId",
                                processRecord.getQuotationId());

                params.addValue(
                                "requestJson",
                                processRecord.getRequestJson());

                params.addValue(
                                "statusId",
                                processRecord.getStatusId());

                params.addValue(
                                "processStep",
                                processRecord.getProcessStep());

                params.addValue(
                                "retryCount",
                                processRecord.getRetryCount());

                params.addValue(
                                "isRetryable",
                                processRecord.getIsRetryable());

                params.addValue(
                                "isActive",
                                processRecord.getIsActive());

                params.addValue(
                                "hasFinalShipment",
                                processRecord.getHasFinalShipment());

                int rowsAffected = jdbcTemplate.update(
                                sql,
                                params);

                if (rowsAffected == 0) {
                        throw new RuntimeException(
                                        "No se encontró SHOPIFY_ORDER con orderNumber="
                                                        + processRecord.getOrderNumber());
                }

                log.info(
                                "[SKYDROPX-PROCESS-DAO] process creado quotationId={}",
                                processRecord.getQuotationId());

                return (long) rowsAffected;
        }

        @Override
        public void updateQuotationCompleted(
                        String quotationId,
                        String quotationRawJson) {
                String sql = "UPDATE SKYDROPX_PROCESS " +
                                "SET " +
                                "quotationRawJson = :quotationRawJson, " +
                                "quotationStatus = 'COMPLETED', " +
                                "totalRates = 0, " +
                                "successRates = 0, " +
                                "errorRates = 0, " +
                                "processStep = :processStep, " +
                                "updatedAt = GETDATE() " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "quotationId",
                                quotationId);

                params.addValue(
                                "quotationRawJson",
                                quotationRawJson);

                params.addValue(
                                "processStep",
                                SkyDropXProcessStep.QUOTATION_COMPLETED);

                jdbcTemplate.update(
                                sql,
                                params);
        }

        @Override
        public void updateSelectedRate(
                        String quotationId,
                        String selectedRateId,
                        String selectedRateJson) {

                String sql = "UPDATE SKYDROPX_PROCESS " +
                                "SET " +
                                "selectedRateId = :selectedRateId, " +
                                "selectedRateJson = :selectedRateJson, " +
                                "provider = JSON_VALUE(:selectedRateJson, '$.provider_name'), " +
                                "serviceLevel = JSON_VALUE(:selectedRateJson, '$.provider_service_name'), " +
                                "totalAmount = TRY_CAST(JSON_VALUE(:selectedRateJson, '$.total') AS DECIMAL(18,2)), " +
                                "estimatedDays = TRY_CAST(JSON_VALUE(:selectedRateJson, '$.days') AS INT), " +
                                "processStep = :processStep, " +
                                "updatedAt = GETDATE() " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "quotationId",
                                quotationId);

                params.addValue(
                                "selectedRateId",
                                selectedRateId);

                params.addValue(
                                "selectedRateJson",
                                selectedRateJson);

                params.addValue(
                                "processStep",
                                SkyDropXProcessStep.RATE_SELECTED);

                jdbcTemplate.update(
                                sql,
                                params);
        }

        @Override
        public void updateShipment(
                        String quotationId,
                        String shipmentId,
                        String shipmentRawJson,
                        String trackingNumber,
                        String labelUrl) {

                String sql = "UPDATE SKYDROPX_PROCESS " +
                                "SET " +
                                "shipmentId = :shipmentId, " +
                                "shipmentRawJson = :shipmentRawJson, " +
                                "trackingNumber = :trackingNumber, " +
                                "labelUrl = :labelUrl, " +
                                "hasFinalShipment = 1, " +
                                "processStep = :processStep, " +
                                "updatedAt = GETDATE() " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "quotationId",
                                quotationId);

                params.addValue(
                                "shipmentId",
                                shipmentId);

                params.addValue(
                                "shipmentRawJson",
                                shipmentRawJson);

                params.addValue(
                                "trackingNumber",
                                trackingNumber);

                params.addValue(
                                "labelUrl",
                                labelUrl);

                params.addValue(
                                "processStep",
                                SkyDropXProcessStep.SHIPMENT_COMPLETED);

                jdbcTemplate.update(
                                sql,
                                params);
        }

        @Override
        public void markCompleted(
                        String quotationId) {

                String sql = "UPDATE SKYDROPX_PROCESS " +
                                "SET " +
                                "statusId = :statusId, " +
                                "processStep = :processStep, " +
                                "completedAt = GETDATE(), " +
                                "updatedAt = GETDATE() " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "quotationId",
                                quotationId);

                params.addValue(
                                "statusId",
                                SkyDropXProcessStatus.COMPLETED);

                params.addValue(
                                "processStep",
                                SkyDropXProcessStep.COMPLETED);

                jdbcTemplate.update(
                                sql,
                                params);
        }

        @Override
        public Optional<SkyDropXProcessRecord> findByQuotationId(
                        String quotationId) {

                String sql = "SELECT * " +
                                "FROM SKYDROPX_PROCESS " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "quotationId",
                                quotationId);

                List<SkyDropXProcessRecord> result = jdbcTemplate.query(
                                sql,
                                params,
                                BeanPropertyRowMapper.newInstance(
                                                SkyDropXProcessRecord.class));

                return result.stream().findFirst();
        }

        @Override
        public void markFailed(
                        String quotationId,
                        String errorMessage) {

                String sql = "UPDATE SKYDROPX_PROCESS " +
                                "SET " +
                                "statusId = :statusId, " +
                                "lastErrorMessage = :errorMessage, " +
                                "updatedAt = GETDATE() " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "quotationId",
                                quotationId);

                params.addValue(
                                "statusId",
                                SkyDropXProcessStatus.FAILED);

                params.addValue(
                                "errorMessage",
                                errorMessage);

                jdbcTemplate.update(
                                sql,
                                params);

                log.warn(
                                "[SKYDROPX-PROCESS] process FAILED quotationId={}, error={}",
                                quotationId,
                                errorMessage);
        }

        @Override
        public void markSupersededByOrderNumber(
                        String orderNumber) {

                String sql = "UPDATE sp " +
                                "SET " +
                                "sp.statusId = :statusId, " +
                                "sp.isActive = 0, " +
                                "sp.completedAt = GETDATE(), " +
                                "sp.updatedAt = GETDATE() " +
                                "FROM SKYDROPX_PROCESS sp " +
                                "INNER JOIN SHOPIFY_ORDER so " +
                                "ON so.idShopifyOrder = sp.idShopifyOrder " +
                                "WHERE so.orderNumber = :orderNumber " +
                                "AND sp.isActive = 1";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue(
                                "orderNumber",
                                orderNumber);

                params.addValue(
                                "statusId",
                                SkyDropXProcessStatus.SUPERSEDED);

                jdbcTemplate.update(
                                sql,
                                params);

                log.info(
                                "[SKYDROPX-PROCESS] workflows SUPERSEDED orderNumber={}",
                                orderNumber);
        }

        @Override
        public List<SkyDropXProcessRecord> findStuckProcesses(
                        int stuckMinutes,
                        int maxRetries) {

                String sql = "SELECT * FROM SKYDROPX_PROCESS " +
                                "WHERE statusId IN (:statusIds) " +
                                "AND isActive = 1 " +
                                "AND isRetryable = 1 " +
                                "AND processStep IN (:steps) " +
                                "AND retryCount < :maxRetries " +
                                "AND updatedAt < DATEADD(minute, -:stuckMinutes, GETDATE())";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue("statusIds", List.of(
                                SkyDropXProcessStatus.PROCESSING,
                                SkyDropXProcessStatus.FAILED));

                params.addValue("steps", List.of(
                                SkyDropXProcessStep.QUOTATION_REQUESTED,
                                SkyDropXProcessStep.QUOTATION_COMPLETED,
                                SkyDropXProcessStep.RATE_SELECTED));

                params.addValue("maxRetries", maxRetries);

                params.addValue("stuckMinutes", stuckMinutes);

                List<SkyDropXProcessRecord> result = jdbcTemplate.query(
                                sql,
                                params,
                                BeanPropertyRowMapper.newInstance(SkyDropXProcessRecord.class));

                log.info("[SKYDROPX-RECOVERY-DAO] procesos atorados encontrados={}", result.size());

                return result;
        }

        @Override
        public void incrementRetryCount(String quotationId) {

                String sql = "UPDATE SKYDROPX_PROCESS " +
                                "SET retryCount = retryCount + 1, " +
                                "updatedAt = GETDATE() " +
                                "WHERE quotationId = :quotationId";

                MapSqlParameterSource params = new MapSqlParameterSource();

                params.addValue("quotationId", quotationId);

                jdbcTemplate.update(sql, params);

                log.info("[SKYDROPX-RECOVERY-DAO] retryCount incrementado quotationId={}", quotationId);
        }

        @Transactional
        @Override
        public void supersedePreviousAndCreate(
                        String orderNumber,
                        SkyDropXProcessRecord processRecord) {

                markSupersededByOrderNumber(orderNumber);
                createProcess(processRecord);
        }
}