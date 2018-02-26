/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2013 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.incaa;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.b3p.kar.hibernate.Movement;
import nl.b3p.kar.hibernate.MovementActivationPoint;
import nl.b3p.kar.hibernate.RoadsideEquipment;
import nl.b3p.kar.hibernate.VehicleType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class IncaaExport {

    private static final Log log = LogFactory.getLog(IncaaExport.class);
    private static final char TAB = '\t';

    public IncaaExport() {
    }

    public File convert(RoadsideEquipment rseq) {
        return convert(Collections.singletonList(rseq));
    }

    public File convert(List<RoadsideEquipment>rseqs) {
        File f = null;
        PrintWriter pw = null;
        try {
            f = File.createTempFile("temp", ".ptx");
            pw = new PrintWriter(f);
            for (RoadsideEquipment roadsideEquipment : rseqs) {
                writeRseq(roadsideEquipment, pw);
            }
        } catch (IOException fne) {
            log.error("Kan RSEQ niet naar incaa bestand schrijven: ", fne);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
        return f;
    }

    private void writeRseq(RoadsideEquipment rseq, PrintWriter pw) {
        for (Movement movement : rseq.getMovements()) {
            String type = movement.determineVehicleType();
            if(type == null || type.equals(VehicleType.VEHICLE_TYPE_OV)){
                continue; 
            }
            for (MovementActivationPoint map : movement.getPoints()) {
                String vehicleType = map.determineVehicleType();
                if(vehicleType == null || !vehicleType.equals(VehicleType.VEHICLE_TYPE_OV)){
                    
                    String line = "";
                    line += rseq.getDataOwner().getCode();  // 1
                    line += TAB;
                    line += map.getPoint().getX(); // 2
                    line += TAB;
                    line += map.getPoint().getY(); // 3
                    line += TAB;
                    line += rseq.getKarAddress(); // 4
                    line += TAB;
                    if (map.getSignal() != null) {
                        Integer signalGroupNumber = map.getSignal().getSignalGroupNumber();
                        if(signalGroupNumber == null){
                            signalGroupNumber = 0;
                        }
                        line += signalGroupNumber;// 5
                        line += TAB;
                        Integer distance = 0;
                        if(map.getSignal().getDistanceTillStopLine() != null){
                            distance = map.getSignal().getDistanceTillStopLine();
                        }
                        line += distance;// 6
                        line += TAB;

                        Integer time = 0;
                        if (map.getSignal().getDistanceTillStopLine() != null && distance >= 0) {
                            time = map.getSignal().getDistanceTillStopLine() / 5;
                        }
                        line += time; // 7
                        line += TAB;
                        line += map.getSignal().getKarCommandType();
                        line += TAB;
                        String triggerType = "0";
                        switch (map.getSignal().getTriggerType()) {
                            case "STANDARD":
                                triggerType = "0";
                                break;
                            case "FORCED":
                                triggerType = "1";
                                break;
                            case "MANUAL":
                                triggerType = "2";
                                break;
                            default:
                                // ga ervan uit dat het een incaa nummer is;
                                triggerType = map.getSignal().getTriggerType();

                        }
                        line += triggerType;
                     /*   line += TAB;
                        line += map.getPoint().getLabel();
                        line += TAB;
                        line += map.getPoint().getId();*/
                    } else {
                        continue;
                    }
                    pw.println(line);
                }
            }
        }
    }
}
