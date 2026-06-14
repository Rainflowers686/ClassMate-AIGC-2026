# Stage 8A-2 Real Device Proof Record

## Device

- **Model**: vivo X300 Pro
- **OS**: OriginOS 6 / Android 16
- **Software version**: PD2502_A_16.0.25.2_aigctest.W1.0.V000L1

## Model

- **Path**: `/sdcard/1225`
- **Model directory access**: granted
- **File diagnostics**:
  - `modelPath`: exists and readable
  - `tokenizer`: exists and readable
  - `config`: exists and readable

## Results

| Test | Outcome |
|------|---------|
| Text init | success |
| Text generate | success |
| Multimodal support | yes |
| callVit method | present |
| callVit test image | 2×2 RGB, 12 bytes |
| callVit return code | 0 |
| Multimodal generate | success |

## Conclusion

Official on-device BlueLM 3B text generation and multimodal diagnostic bridge were verified on vivo X300 Pro cloud device after granting model directory access.

## Screenshots

Screenshot files are stored externally and are **not committed to this repository**. They will be included in the competition proof pack at the submission stage.

**External path**: `D:\Edge Download\AIGC\ClassMate_proof_assets\stage8a2_ondevice_20260606\screenshots\`

| # | Filename | Content |
|---|----------|---------|
| 1 | `01_DEVICE_x300pro_originos6_hardware_overview.png` | Device hardware overview (vivo X300 Pro, OriginOS 6) |
| 2 | `02_DEVICE_x300pro_android16_system_overview.png` | System overview (Android 16) |
| 3 | `03_DEVICE_x300pro_version_info_android16_aigctest.png` | Software version info (aigctest build) |
| 4 | `04_TEXT_model_dir_granted_files_readable.png` | Model directory access granted, files readable |
| 5 | `05_TEXT_init_generate_success.png` | Text init and generate success |
| 6 | `06_MM_callvit_generate_success.png` | Multimodal callVit and generate success |
