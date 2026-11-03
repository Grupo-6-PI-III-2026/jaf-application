ALTER TABLE pagamento_reembolso ADD COLUMN cancelado_em TIMESTAMP;
ALTER TABLE pagamento_reembolso ADD COLUMN cancelado_por BIGINT REFERENCES funcionario(id);
ALTER TABLE pagamento_reembolso ADD COLUMN motivo_cancelamento VARCHAR(500);
