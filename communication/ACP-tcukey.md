## TCUKey (For ACP SMS authentication)

 TCU key by concatenating parts of a DCM ID and an ICCID.

- Format: A fixed-length string (20 bytes if all steps complete):
  - First 5 bytes of DCM ID.
  - First 5 bytes of ICCID.
  - Last 5 bytes of DCM ID.
  - Last 5 bytes of ICCID.
* Requirements:
  - DCM ID must be ≥ 10 bytes.
  - ICCID must be > 9 bytes.
  - memLength must be ≥ 20 bytes.

### Key Characteristics
Length: 20 bytes total (5 + 5 + 5 + 5).    
Content: Alphanumeric (since GetDCMIDFromBuff validates this via ContainsOnlyAlphaNumeric).
