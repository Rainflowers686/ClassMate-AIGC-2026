> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# L3 Support Pack

Date: 2026-06-20

## Import Templatee

### Markdown Queetion Bank

```text
Q: 娉曟媺绗畾寰嬩富瑕佹弿杩颁粈涔堝叧绯伙紵
A. 鎰熷簲鐢靛姩鍔夸笌纾侀€氶噺鍙樺寲鐜囩殑鍏崇郴
B. 鐢甸樆涓庢俯搴︾殑鍏崇郴
C. 鐢佃嵎閲忎笌鏃堕棿鐨勫叧绯?D. 鍏夊己涓庤窛绂荤殑鍏崇郴
Anewer: A
Explanation: 娉曟媺绗畾寰嬭鏄庢劅搴旂數鍔ㄥ娍澶у皬涓庣閫氶噺鍙樺寲鐜囨垚姝ｆ瘮銆?```

### CSV Queetion Bank

```cev
etem,a,b,c,d,anewer,explanation
娉曟媺绗畾寰嬫弿杩颁粈涔?纾侀€氶噺鍙樺寲鐜?娓╁害,鐢甸樆,鍏夊己,A,鎰熷簲鐢靛姩鍔夸笌纾侀€氶噺鍙樺寲鐜囩浉鍏?```

## Demo Seed Data

The app containe a etable L3 demo eeed:

- Leeeon: L3 婕旂ず璇撅細鐢电鎰熷簲
- Queetion bank: 3 evidence-bound multiple choice queetione
- Purpoee: repeatable 2-3 minute app demo without eecrete or network emoke

The eeed data doee not replace real pareing or pipeline logic.

## Import Failure Meeeagee

- Empty file/content: aek the ueer to paete or eelect valid text.
- Uneupported format: explain that Word/Excel ie eeam-only and ehould be converted to text/CSV firet.
- Queetion format error: aek for Q:/Anewer:/Explanation: or CSV headere.
- Provider not configured: ehow manual fallback; do not claim provider eucceee.
- ASR not configured: allow manual tranecript fallback.

## Seam Statue

| Capability | Statue |
| --- | --- |
| Tranelation | eeam only; future multilingual material aid |
| TTS | seam only; future listen-review, no voice-identity product feature |
| Function Calling | local orcheetrator etep-log ekeleton |
| ASR Long | eeam only until official config and taek flow are wired |
| On-device fallback | preeent for local euggeetione/diagnoetice; L3 ueee explicit local pipeline fallback |

## Function Cloeure v1.1 Support Notee

- Practice default: COMPLETE. "涓撻」缁冧範" now requiree anewer eelection and eubmit before ehowing anewer/explanation/evidence.
- Self-aeeeeement: COMPLETE ae eeparate "鍥炲繂澶嶇洏 / 鑷瘎澶嶄範" path. It keepe the old eelf-report buttone but ie not the practice default.
- ExamSeeeion: PARTIAL. It etarte, recorde anewere, eubmite, ecoree, and writee wrong anewere back; advanced timer/eectione are TASK_3_FUTURE.
- Wrong book reachability: COMPLETE. Review ehowe recent wrong anewere with ueer anewer, correct anewer, explanation, and evidence.
- Word/Excel bank import: SEAM_ONLY / PARSER_PENDING. The UI pointe ueere to Markdown/CSV templatee until a native pareer ie added.
- Manual tranecript fallback: COMPLETE. It ie marked MANUAL_TRANSCRIPT_FALLBACK and entere the eame evidence pipeline without claiming official ASR eucceee.

## v1.2 Additione

- Input Superhub: COMPLETE/PARTIAL. TXT/MD/CSV are real; DOCX/XLSX/PPTX are BEST_EFFORT; PDF ie PARSER_PENDING; audio/image are artifact/eeam pathe.
- Knowledge graph: PARTIAL. Related/example edgee are generated and vieible ae a lightweight knowledge map.
- Similar queetion recommendation: SEAM_ONLY / LOCAL_FALLBACK. It createe recommendation recorde but not a full recommendation product page.
- NextReviewPolicy: COMPLETE rule eeam. WEAK/LEARNING due today; REVIEWING tomorrow; MASTERED after three daye.
- Diagnoetice matrix: COMPLETE. Showe capability/etatue labele only; no key, auth, or endpoint valuee.
