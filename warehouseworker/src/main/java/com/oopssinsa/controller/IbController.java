package com.oopssinsa.controller;

import com.oopssinsa.model.dto.*;
import com.oopssinsa.model.service.IbService;
import com.oopssinsa.model.service.LocationService;
import com.oopssinsa.model.service.StockService;
import com.oopssinsa.view.IbInstructionView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IbController {
    private String workerId=null;
    IbService ibService = new IbService();
    StockService stockService = new StockService();
    LocationService locationService = new LocationService();
    IbInstructionView ibInstructionView = new IbInstructionView();

    public void setWorkerId(String id){
        this.workerId =id;
    }

    public void getIbInstructionToDo(){
        List<IbInstructionDto> ibInstructions = ibService.getIbInstructionToDo(this.workerId);
        if(ibInstructions!=null){
            ibInstructionView.displayIbInstructions(ibInstructions);
        }
        else{
            System.out.println("입고 지시 목록 오류 발생");
        }
    }
    public void updateIbStatus(){

        //유효하지 않은 정수 값 -1로 초기화
        long ibInstructionId =-1;
        String productId = null;
        LocalDate manufactureId = null;

        //처리할 입고 내역을 찾을 id, 상품
        String updateStatus = ibInstructionView.inputUpdateIbInstructionStatus(ibInstructionId, productId, manufactureId);
        //location id는 상태 업데이트와 관련 없으므로 0 전달
        IbDto updateIb = new IbDto(ibInstructionId, manufactureId, productId, 0, LocalDate.now(),updateStatus);
        //재귀 쿼리로 해당 입고 요청 찾아 상태 업데이트
        ibService.updateIbStatus(updateIb);

        // 입고 처리 결과가 성공이면 재고 현황, 재고처리 내역, 구역별 용량 업데이트
        if(updateStatus=="S"){
            IbDto foundIb = ibService.findIb(new IbDto(ibInstructionId, manufactureId, productId, 0,null,null));
            IbInstructionDto foundIbInstruction = ibService.findIbInstruction(new IbInstructionDto(ibInstructionId, manufactureId,productId,this.workerId,0));
            StockDto foundStock = stockService.findStock(new StockDto(productId, manufactureId, 0,0,0));
            int updateQuantity; // 입고 요청의 수량
            int originalQuantity; // 원래 재고 수량
            int expectedQuantity; // 원래 재고 예정 수량(작업 예정 수량)
            long locationId = foundIbInstruction.getLocationId();
            int originalCapacity = locationService.getCurrentCapacity(locationId);
            if(foundIb!=null && foundStock!=null){
                updateQuantity = foundIb.getQuantity();
                originalQuantity = foundStock.getQuantity();
                expectedQuantity = foundStock.getExpected_quantity();
                stockService.updateStock(new StockDto(productId, manufactureId, 0, originalQuantity+updateQuantity,
                        expectedQuantity-updateQuantity));
                stockService.insertStockHistory(new StockHistoryDto(manufactureId,productId,updateQuantity,LocalDate.now()));
                locationService.updateCurrentCapacity(new SubLocationDto(locationId,originalCapacity+updateQuantity,0));

            }

        }

    }
}
