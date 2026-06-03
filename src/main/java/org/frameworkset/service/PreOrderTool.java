package org.frameworkset.service;
/**
 * Copyright 2026 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/21
 */
public class PreOrderTool {
    @Tool(name="hotelQuery",description = "根据用户的行程需求，查询合适的酒店。"
    )
    public List<Map> hotelQuery(@ToolParam(name="startDay",description = "入驻时间,例如：5月25日",required = true) String startDay,
                                @ToolParam(name="endDay",description = "离房时间,例如：5月28日",required = true) String endDay){
        List<Map> hotels = new ArrayList<>();
        Map hotelData = new LinkedHashMap();
        hotelData.put("name","迁移山水酒店");
        hotelData.put("price","300$");
        hotelData.put("score",80);
        hotelData.put("devices","配套设施：健身房、保龄球");
        hotelData.put("position","位于市中心，交通便利");
        hotels.add(hotelData);

        hotelData = new LinkedHashMap();
        hotelData.put("name","俊逸酒店");
        hotelData.put("price","400$");
        hotelData.put("score",90);
        hotelData.put("devices","配套设施：健身房、保龄球、羽毛球");
        hotelData.put("position","位于郊区，环境优雅");
        hotels.add(hotelData);


        hotelData = new LinkedHashMap();
        hotelData.put("name","华天大酒店");
        hotelData.put("price","500$");
        hotelData.put("score",95);
        hotelData.put("devices","配套设施：健身房、保龄球、羽毛球、游泳池");
        hotelData.put("position","位于郊区，环境优雅，五星级环境");
        hotels.add(hotelData);
        return hotels;

    }

    @Tool(name="flightQuery",description = "根据用户的行程需求，查询合适的航班机票。" )
    public List<Map> flightQuery(@ToolParam(name="bookDay",description = "出发时间,例如：5月25日",required = true) String bookDay,
                                 @ToolParam(name="arriveDay",description = "到达时间,例如：5月28日",required = true) String arriveDay,
                                 @ToolParam(name="fromStation",description = "出发地,例如：长沙",required = true) String fromStation,
                                 @ToolParam(name="toStation",description = "到达地,例如：北京",required = true) String toStation){
        List<Map> hotels = new ArrayList<>();
        Map hotelData = new LinkedHashMap();
        hotelData.put("name","国航6678");
        hotelData.put("price","300$");
        hotelData.put("score",80);
        hotelData.put("devices","波音777");

        hotelData.put("leaveTime","14点30分");
        hotelData.put("arrivedTime","17点30分");
        hotelData.put("description","宽体大飞机，准点率99%");
        hotels.add(hotelData);

        hotelData = new LinkedHashMap();
        hotelData.put("name","南航5578");
        hotelData.put("price","400$");
        hotelData.put("score",70);
        hotelData.put("devices","空壳380");

        hotelData.put("leaveTime","15点30分");
        hotelData.put("arrivedTime","18点30分");
        hotelData.put("description","宽体大飞机，准点率90%");
        hotels.add(hotelData);


        hotelData = new LinkedHashMap();
        hotelData.put("name","厦门航空3378");
        hotelData.put("price","300$");
        hotelData.put("score",80);
        hotelData.put("devices","波音730");

        hotelData.put("leaveTime","16点30分");
        hotelData.put("arrivedTime","18点30分");
        hotelData.put("description","宽体大飞机，准点率100%");
        hotels.add(hotelData);
        return hotels;

    }

    

}
