# HondaLink API Commands Reference

Extracted from HondaLink Android APK v5.0.54 via JADX decompilation.

All remote commands use the generic Retrofit endpoint:
```
POST /REST/NGT/CIG/{eng}/async/{command}
```

## Remote Vehicle Commands

All commands are sent via `POST` to `wsc.hondaweb.com` with CIG auth headers.
Body format: `{"device": "<VIN>", "pin": "<PIN>", ...}`

### Door Lock/Unlock

| Action | Path Param (`eng`) | Command | Full Path | MQTT Shadow |
|---|---|---|---|---|
| Lock | `lk` | `alk` | `/REST/NGT/CIG/lk/async/alk` | (no dedicated shadow) |
| Unlock | `lk` | `dulk` | `/REST/NGT/CIG/lk/async/dulk` | (no dedicated shadow) |

### Climate (Engine Start/Stop)

| Action | Path Param (`eng`) | Command | Full Path | MQTT Shadow |
|---|---|---|---|---|
| Start Climate | `eng` | `srt` | `/REST/NGT/CIG/eng/async/srt` | `ENGINE_START_STOP_ASYNC` |
| Stop Climate | `eng` | `sop` | `/REST/NGT/CIG/eng/async/sop` | `ENGINE_START_STOP_ASYNC` |

Additional body fields for climate start:
```json
{
  "device": "<VIN>",
  "pin": "<PIN>",
  "extend": false,
  "vehicleControl": {
    "acSetting": {
      "acDefSetting": "autoOn",
      "acTempVal": "72"
    }
  }
}
```

Climate stop uses `"acDefSetting": "autoOff"`.

### Car Finder (Light Flash / Horn Honk)

| Action | Path Param (`eng`) | Command | Full Path | MQTT Shadow |
|---|---|---|---|---|
| Flash Lights | `cfhl` | `lgt` | `/REST/NGT/CIG/cfhl/async/lgt` | `CARFINDER_HORN_LIGHT_ASYNC` |
| Honk Horn | `cfhl` | `hrn` | `/REST/NGT/CIG/cfhl/async/hrn` | `CARFINDER_HORN_LIGHT_ASYNC` |
| **Stop Both** | `cfhl` | `sop` | `/REST/NGT/CIG/cfhl/async/sop` | `CARFINDER_HORN_LIGHT_ASYNC` |

Note: The stop command (`sop`) cancels both lights and horn. There is no separate stop per feature.

### Vehicle Locator

| Action | Path Param (`eng`) | Command | Full Path |
|---|---|---|---|
| Locate Vehicle | `vehiclelocaterAsync` | (empty) | `/REST/NGT/CIG/vehiclelocaterAsync/async/` |
| Locate Vehicle | `cfl` | (empty) | `/REST/NGT/CIG/cfl/async/` |

Note: Two different implementations exist in the app (v6/u.java and v6/C4835a.java). The `cfl` variant is used by "Find My Car" / Agero integration.

### Dashboard

| Action | Path Param (`eng`) | Command | Full Path | MQTT Shadow |
|---|---|---|---|---|
| Request Dashboard | `dbd` | (empty) | `/REST/NGT/CIG/dbd/async` | `DASHBOARD_ASYNC` |
| Latest Dashboard | - | - | `/REST/NGT/CIG/dbd/latest/{device}`| - |

### Dashboard (Geofence)

| Action | Path Param (`eng`) | Command | Full Path |
|---|---|---|---|
| Geofence | `gefe` | (async) | `/REST/NGT/CIG/gefe/async` |
| Geofence Results| - | - | `/REST/NGT/CIG/gefe/results/{device}` |

### Stolen Vehicle Locator

| Action | Path Param | Full Path |
|---|---|---|
| Report Stolen | - | `/REST/NGT/SVLReportStolenVehicle/1.0` |
| Get SVL Status | - | `/REST/NGT/SVLGetStatus/1.0/{device}` |

### Speed Alert (SPAL)

| Action | Full Path |
|---|---|
| Set Alert | `/REST/NGT/CIG/spal/async` |
| Get Results | `/REST/NGT/CIG/spal/results/{device}` |

### WiFi Hotspot

| Action | Full Path |
|---|---|
| Manage Hotspot | `/REST/NGT/WifiHotspotAsync/1.0` |

### Cabin Temperature (Idle)

Used internally before starting engine to fetch cabin temperature:
```
Path Param: IdleCabinTemperature
```

---

## Non-CIG REST APIs (Direct)

### Charge Management

| Action | Method | Full Path | MQTT Shadow |
|---|---|---|---|
| Set Target Charge | POST | `/REST/NGT/TargetChargeLevel/1.0` | `CHARGEMANAGEMENT_TARGETCHARGELEVEL_ASYNC` |
| Manage Charge | POST | `/REST/NGT/ManageCharge/1.0` | `CHARGEMANAGEMENT_STOPFASTCHARGE_ASYNC` |
| Charge Async Level | POST | `/REST/CIG/ChargeAsyncLevel/1.0` | - |
| Charge Schedule | POST | `/REST/CIG/ChargeSchedule/1.0` | - |
| Charge Start/Stop | POST | `/REST/CIG/ChargeStartStop/1.0/{command}` | - |
| Charger Power Level | POST | `/REST/NGT/ChargerPowerLevel/1.0` | `CHARGEMANAGEMENT_SETCHARGEPOWERLEVEL_ASYNC` |
| Get Charge Power Level| POST | `/REST/NGT/ChargerPowerLevel/1.0` | `CHARGEMANAGEMENT_GETCHARGEPOWERLEVEL_ASYNC` |

### Climate (Alternative CIG endpoints)

| Action | Method | Full Path |
|---|---|---|
| Climate Setting | POST | `/REST/CIG/ClimateSetting/1.0` |
| Climate Async | POST | `/REST/CIG/ClimateAsync/1.0/{command}` |
| Get Climate Result | GET | `/REST/NGT/getClimateResult/1.0/{command}/{requestId}` |
| Get Climate Status | GET | `/REST/NGT/getClimateStatus/1.0/{vin}` |

### Commute Schedule

| Action | Method | Full Path |
|---|---|---|
| Set Schedule | POST | `/REST/NGT/setCommuteSchedule/1.0` |

### Vehicle Information

| Action | Method | Full Path |
|---|---|---|
| Get Vehicles | GET | `/REST/NGT/MyVehicle/1.0` |
| Add/Update Vehicle | POST | `/REST/NGT/MyVehicle/1.0` |
| VIN Lookup | GET | `/REST/NGT/VinLookUp/1.0/{VIN}` |
| Feature List | GET | `/REST/NGT/FeatureList/1.0/{VIN}` |
| Vehicle Profile | GET | `/REST/NGT/myProfile/1.0/{VIN}` |
| Activity Log | GET | `/REST/NGT/ActivityLog/1.0/{device}` |
| Vehicle Messages | GET | `/REST/NGT/VehicleMessage/Details/1.0/{VIN}` |
| Message Status | POST | `/REST/NGT/VehicleMessage/Status/1.0` |

### Authentication & Token

| Action | Method | Full Path |
|---|---|---|
| Register Client | POST | `/hidas/rs/client/register` |
| Generate Token | POST | `/hidas/rs/token/generate` |
| CIG Token | POST | `/REST/CIG/services/1.0/token` |
| Auth Challenge | POST | `/REST/NGT/AuthChallenge/1.0` |

### PIN Management

| Action | Method | Full Path |
|---|---|---|
| Update PIN | PUT | `/REST/NGT/PinManagement/UpdatePinService/1.0/` |
| Enrollment PIN | POST | `/REST/NGT/EnrollmentPIN/1.0/{VIN}` |

### CIG Command Results (Polling)

| Action | Method | Full Path |
|---|---|---|
| Get Results | GET | `/REST/NGT/CIG/{eng}/results/{requestId}` |
| Get Charge Result | GET | `/REST/NGT/getChargeResult/1.0/{command}/{requestId}` |

### Notifications

| Action | Method | Full Path |
|---|---|---|
| Register Push Token | POST | `/REST/NGT/NE/pushToken/2.0/` |
| Update Push Token | PUT | `/REST/NGT/NE/pushToken/2.0/` |
| CIG Notification | POST | `/REST/NGT/CIGNotification/1.0/{sxmId}/{device}` |
| Get Preferences | GET | `/REST/NGT/NS/preference/{device_id}` |
| Update Preferences | PUT | `/REST/NGT/NS/preference/{preference_id}` |

---

## MQTT Shadow Topics

All topics follow the pattern:
```
$aws/things/thing_{VIN}/shadow/name/{SHADOW_NAME}/{action}
```

| Shadow Name | Used For |
|---|---|
| `DASHBOARD_ASYNC` | Dashboard data updates |
| `ENGINE_START_STOP_ASYNC` | Climate start/stop confirmation |
| `CHARGEMANAGEMENT_TARGETCHARGELEVEL_ASYNC` | Charge target confirmation |
| `CARFINDER_HORN_LIGHT_ASYNC` | Light/horn command confirmation |
| `CARFINDER_LOCATION_ASYNC` | Vehicle locator results |
| `CHARGEMANAGEMENT_STOPFASTCHARGE_ASYNC` | Stop fast charge confirmation |
| `CHARGEMANAGEMENT_SETCHARGEPOWERLEVEL_ASYNC` | Set charge power level |
| `CHARGEMANAGEMENT_GETCHARGEPOWERLEVEL_ASYNC` | Get charge power level |
| `CHARGEMANAGEMENT_UPDATEPROFILE_ASYNC` | Update charge profile |
| `CLIMATE_ASYNC` | Climate setting changes |
| `CLIMATE_SETTING_ASYNC` | Climate setting preferences |

Actions: `/update`, `/update/documents`, `/get`, `/get/accepted`

---

## CIG Polling Manager (C4837c)

The core command execution class. Constructor signature:
```java
C4837c(CIGPostRequest request, callback, String pathParam, String command, String pin)
```

The URL is built as: `/REST/NGT/CIG/{pathParam}/async/{command}`

Known `pathParam` + `command` combinations:

| pathParam | command | Description |
|---|---|---|
| `eng` | `srt` | Engine/Climate Start |
| `eng` | `sop` | Engine/Climate Stop |
| `lk` | `alk` | Door Lock (All Lock) |
| `lk` | `dulk` | Door Unlock |
| `cfhl` | `lgt` | Flash Lights |
| `cfhl` | `hrn` | Honk Horn |
| `cfhl` | `sop` | Stop Light/Horn |
| `cfl` | (empty) | Car Finder / Locate |
| `dbd` | (empty) | Dashboard Request |
| `gefe` | (async) | Geofence |
| `vehiclelocaterAsync` | (empty) | Vehicle Locator (async) |
| `IdleCabinTemperature` | (empty) | Cabin Temperature Query |
| `spal` | (async) | Speed Alert |
| `wph` | (async) | WiFi Hotspot |
| `pi` | (async) | Parked In (location) |

---

*Generated from HondaLink v5.0.54 APK decompilation on 2026-02-24*
