package com.creditienda.service.delivery.core;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.creditienda.dto.delivery.B2BActualizarEstatusEntregaDTO;
import com.creditienda.dto.delivery.B2BSeguimientoEntregaOrdenDTO;
import com.creditienda.service.delivery.dao.DeliveryDAO;

@Service
public class DeliveryCoreService {

    private final DeliveryDAO dao;

    public DeliveryCoreService(DeliveryDAO dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public List<B2BSeguimientoEntregaOrdenDTO> consultarOrdenesPorEstatus(List<Integer> idEstatusOC,
            List<Integer> idEstatusDeliverys) {

        return dao.findByEstatus(idEstatusOC, idEstatusDeliverys);
    }

    @Transactional
    public void updateEstatusDelivery(B2BActualizarEstatusEntregaDTO dto) {
        dao.updateEstatusDelivery(dto);
    }
}