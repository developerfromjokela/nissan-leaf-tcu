## CAN for 2016-2019 vehicles (draft)

UDS commands, same RX&TX? Not tested in a vehicle!

Supported UDS diag commands:

- 0x31
- 0x2e
- 0x21
- 0x30
- 0x34
- 0x3b
- 0x32

```cpp

void UDS_DiagIDs(uint param_1)

{
  if (param_1 == 0x6c95) {
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_SIMISDN_0040cabc,1);
  }
  if (param_1 < 0x6c96) {
    if (param_1 == 0x6c2f) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_ECALLAUTOACT_0040c902,1);
    }
    if (0x6c2f < param_1) {
      if (param_1 == 0x6c6d) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_EVSERVACTSTATUS_0040c9ce,1);
      }
      if (0x6c6d < param_1) {
        if (param_1 == 0x6c72) {
                    /* WARNING: Subroutine does not return */
          UARTLog(1,s_DATARW_ID_EVCONFIGDEFINITION_0040ca37,1);
        }
        if (param_1 < 0x6c73) {
          if (param_1 == 0x6c6f) {
                    /* WARNING: Subroutine does not return */
            UARTLog(2,s_DATARW_ID_EVSERVICEPROVISIONING_0040ca03,1);
          }
          if (param_1 < 0x6c6f) {
                    /* WARNING: Subroutine does not return */
            UARTLog(2,s_DATARW_ID_EVSTATUSDATAFREQ_0040c9e8,1);
          }
          if (param_1 != 0x6c71) {
            Log_DATARW_ID_Default();
            return;
          }
                    /* WARNING: Subroutine does not return */
          UARTLog(1,s_DATARW_ID_TCUSTATUS_0040ca23,1);
        }
        if (param_1 == 0x6c76) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_EVCHRGINPROGRESSTO_0040ca6b,1);
        }
        if (param_1 < 0x6c77) {
          if (param_1 != 0x6c74) {
            Log_DATARW_ID_Default();
            return;
          }
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_EVWAKEUPFREQ_0040ca54,1);
        }
        if (param_1 == 0x6c8c) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_EVBSOCDIFFFORTX_0040ca88,1);
        }
        if (param_1 != 0x6c8e) {
          Log_DATARW_ID_Default();
          return;
        }
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_VSOCTXFREQUENCY_0040caa2,1);
      }
      if (param_1 == 0x6c3c) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SIMUSERID_0040c962,1);
      }
      if (param_1 < 0x6c3d) {
        if (param_1 == 0x6c38) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_SIMUSERID2_0040c936,1);
        }
        if (param_1 == 0x6c3a) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_SIMPASSWORD2_0040c94b,1);
        }
        if (param_1 != 0x6c31) {
          Log_DATARW_ID_Default();
          return;
        }
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_ECALLNUMVALIDATION_0040c919,1);
      }
      if (param_1 == 0x6c44) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SIMPIN_0040c98c,1);
      }
      if (param_1 < 0x6c45) {
        if (param_1 != 0x6c3d) {
          Log_DATARW_ID_Default();
          return;
        }
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SIMPASSWORD_0040c976,1);
      }
      if (param_1 == 0x6c46) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SIMPINUSE_0040c99d,1);
      }
      if (param_1 != 0x6c6a) {
        Log_DATARW_ID_Default();
        return;
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_PROBESERVACTSTATUS_0040c9b1,1);
    }
    if (param_1 == 0x6c22) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_SIMSMSCENTER_0040c832,1);
    }
    if (0x6c22 < param_1) {
      if (param_1 == 0x6c29) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_ECALLNUMSELECTION_0040c884,1);
      }
      if (param_1 < 0x6c2a) {
        if (param_1 == 0x6c26) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_ECALLNUMOPERATIONAL_0040c849,1);
        }
        if (param_1 == 0x6c28) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_ECALLLIFECYCLETEST_0040c867,1);
        }
        if (param_1 != 0x6c24) {
          Log_DATARW_ID_Default();
        }
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SIMSMSCENTER_0040c832,1);
      }
      if (param_1 == 0x6c2b) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_BCALLNUMBER_0040c8bc,1);
      }
      if (param_1 < 0x6c2b) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_ECALLNUMSELECTENG_0040c8a0,1);
      }
      if (param_1 == 0x6c2c) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_CCALLNUMBER_0040c8d2,1);
      }
      if (param_1 != 0x6c2e) {
        Log_DATARW_ID_Default();
        return;
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_ECALLBCALLDEACT_0040c8e8,1);
    }
    if (param_1 == 0x6c02) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_ECALLVEHICLETYPE_0040c7d8,1);
    }
    if (0x6c02 < param_1) {
      if (param_1 == 0x6c10) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_OBSURL_0040c80f,1);
      }
      if (param_1 == 0x6c12) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_OBSPORT_0040c820,1);
      }
      if (param_1 != 0x6c04) {
        Log_DATARW_ID_Default();
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_ECALLVEHPROPULSTG_0040c7f3,1);
    }
    if (param_1 == 0x111) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_BLOCKCHARGESTATUS_0040c7a2,1);
    }
    if (param_1 == 0x124) {
                    /* WARNING: Subroutine does not return */
      UARTLog(1,s_DATARW_ID_MMISUPPLYOUTPUT_0040c7be,1);
    }
    if (param_1 != 0x107) {
      Log_DATARW_ID_Default();
    }
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_VEHICLEDEFINITION_0040c786,1);
  }
  if (param_1 == 0xfd0e) {
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_GSMCALLPARAM_0040cbb0,1);
  }
  if (0xfd0e < param_1) {
    if (param_1 == 0xfd2c) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_SIMSMSPARAMETERS_0040cc7d,1);
    }
    if (0xfd2c < param_1) {
      if (param_1 == 0xfd33) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_TCUPRIVATEKEY_0040cc10,1);
      }
      if (param_1 < 0xfd34) {
        if (param_1 == 0xfd31) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_CACERTIFICATE_0040cbdf,1);
        }
        if (0xfd31 < param_1) {
                    /* WARNING: Subroutine does not return */
          UARTLog(2,s_DATARW_ID_TCUCERTIFICATE_0040cbf7,1);
        }
        if (param_1 != 0xfd30) {
          Log_DATARW_ID_Default();
          return;
        }
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SIMGPRSPARAMETERS_0040cc98,1);
      }
      if (param_1 == 0xfd62) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_MAINSERVERCONFIG_0040ccb4,1);
      }
      if (param_1 < 0xfd63) {
        if (param_1 != 0xfd34) {
          Log_DATARW_ID_Default();
          return;
        }
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_SHAREDKEYSMS_0040cc28,1);
      }
      if (param_1 == 0xfe99) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_HWREFERENCE_0040cd7c,1);
      }
      if (param_1 != 0xfe9a) {
        Log_DATARW_ID_Default();
        return;
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_TCU_UNIQUE_0040cd92,1);
    }
    if (param_1 == 0xfd19) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_TCUPRIVATEKEY_0040cc10,1);
    }
    if (param_1 < 0xfd1a) {
      if (param_1 == 0xfd17) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_CACERTIFICATE_0040cbdf,1);
      }
      if (0xfd17 < param_1) {
                    /* WARNING: Subroutine does not return */
        UARTLog(2,s_DATARW_ID_TCUCERTIFICATE_0040cbf7,1);
      }
      if (param_1 != 0xfd15) {
        Log_DATARW_ID_Default();
        return;
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_TCUPARAMETERS_0040cbc7,1);
    }
    if (param_1 == 0xfd1c) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_IMEI_0040cc3f,1);
    }
    if (param_1 < 0xfd1d) {
      if (param_1 != 0xfd1b) {
        Log_DATARW_ID_Default();
        return;
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_SHAREDKEYSMS_0040cc28,1);
    }
    if (param_1 == 0xfd20) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_MICSETTINGS2_0040cc4e,1);
    }
    if (param_1 != 0xfd25) {
      Log_DATARW_ID_Default();
      return;
    }
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_SIMPARAMETERS_0040cc65,1);
  }
  if (param_1 == 0x6d11) {
                    /* WARNING: Subroutine does not return */
    UARTLog(1,s_DATARW_ID_SVTD_NMODE_IGNOFF_0040cccf,1);
  }
  if (0x6d11 < param_1) {
    if (param_1 == 0x6d15) {
                    /* WARNING: Subroutine does not return */
      UARTLog(1,s_DATARW_ID_SVTA_NMODE_IGNON_0040cd37,1);
    }
    if (param_1 < 0x6d16) {
      if (param_1 == 0x6d13) {
                    /* WARNING: Subroutine does not return */
        UARTLog(1,s_DATARW_ID_SVTD_VMODE_0040cd06,1);
      }
      if (0x6d13 < param_1) {
                    /* WARNING: Subroutine does not return */
        UARTLog(1,s_DATARW_ID_SVTA_NMODE_IGNOFF_0040cd1b,1);
      }
                    /* WARNING: Subroutine does not return */
      UARTLog(1,s_DATARW_ID_SVTD_NMODE_IGNON_0040cceb,1);
    }
    if (param_1 == 0x6d17) {
                    /* WARNING: Subroutine does not return */
      UARTLog(1,s_DATARW_ID_SVTA_BMODE_0040cd67,1);
    }
    if (param_1 < 0x6d17) {
                    /* WARNING: Subroutine does not return */
      UARTLog(1,s_DATARW_ID_SVTA_VMODE_0040cd52,1);
    }
    if (param_1 == 0xfd05) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_SPEAKERSETTINGS_0040cb7f,1);
    }
    if (param_1 != 0xfd06) {
      Log_DATARW_ID_Default();
      return;
    }
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_MICSETTINGS1_0040cb99,1);
  }
  if (param_1 == 0x6cb2) {
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_BCALLACTSTATUS_0040cb18,1);
  }
  if (0x6cb2 < param_1) {
    if (param_1 == 0x6cc3) {
                    /* WARNING: Subroutine does not return */
      UARTLog(2,s_DATARW_ID_NUMBERCARDINDECT_0040cb48,1);
    }
    if (param_1 == 0x6d10) {
                    /* WARNING: Subroutine does not return */
      UARTLog(1,s_DATARW_ID_ECALLLOCKEDBYDIAG_0040cb63,1);
    }
    if (param_1 != 0x6cc2) {
      Log_DATARW_ID_Default();
      return;
    }
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_WAKEUPBYLEXT_0040cb31,1);
  }
  if (param_1 == 0x6ca8) {
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_EVSMSWAITTIME_0040cae7,1);
  }
  if (param_1 == 0x6cb0) {
                    /* WARNING: Subroutine does not return */
    UARTLog(2,s_DATARW_ID_ECALLACTSTATUS_0040caff,1);
  }
  if (param_1 != 0x6ca5) {
    Log_DATARW_ID_Default();
    return;
  }
                    /* WARNING: Subroutine does not return */
  UARTLog(2,s_DATARW_ID_EVACTTIMECHECK_0040cace,1);
}


```
