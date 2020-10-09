package com.sparrowwallet.sparrow.io;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sparrowwallet.drongo.ExtendedKey;
import com.sparrowwallet.drongo.KeyDerivation;
import com.sparrowwallet.drongo.policy.Policy;
import com.sparrowwallet.drongo.policy.PolicyType;
import com.sparrowwallet.drongo.protocol.ScriptType;
import com.sparrowwallet.drongo.wallet.Keystore;
import com.sparrowwallet.drongo.wallet.KeystoreSource;
import com.sparrowwallet.drongo.wallet.Wallet;
import com.sparrowwallet.drongo.wallet.WalletModel;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class ColdcardSinglesig implements KeystoreFileImport, WalletImport {
    @Override
    public String getName() {
        return "Coldcard";
    }

    @Override
    public String getKeystoreImportDescription() {
        return "Import file created by using the Advanced > MicroSD > Export Wallet > Generic JSON feature on your Coldcard. Note this requires firmware version 3.1.3 or later.";
    }

    @Override
    public WalletModel getWalletModel() {
        return WalletModel.COLDCARD;
    }

    @Override
    public boolean isEncrypted(File file) {
        return false;
    }

    @Override
    public boolean isScannable() {
        return false;
    }

    @Override
    public Keystore getKeystore(ScriptType scriptType, InputStream inputStream, String password) throws ImportException {
        try {
            Gson gson = new Gson();
            Type stringStringMap = new TypeToken<Map<String, JsonElement>>() {
            }.getType();
            Map<String, JsonElement> map = gson.fromJson(new InputStreamReader(inputStream), stringStringMap);

            if (map.get("xfp") == null) {
                throw new ImportException("File was not a valid Coldcard wallet export");
            }

            String masterFingerprint = map.get("xfp").getAsString();

            for (String key : map.keySet()) {
                if (key.startsWith("bip")) {
                    ColdcardKeystore ck = gson.fromJson(map.get(key), ColdcardKeystore.class);

                    if(ck.name != null) {
                        ScriptType ckScriptType = ScriptType.valueOf(ck.name.replace("p2wpkh-p2sh", "p2sh_p2wpkh").toUpperCase());
                        if(ckScriptType.equals(scriptType)) {
                            Keystore keystore = new Keystore();
                            keystore.setLabel(getName());
                            keystore.setSource(KeystoreSource.HW_AIRGAPPED);
                            keystore.setWalletModel(WalletModel.COLDCARD);
                            keystore.setKeyDerivation(new KeyDerivation(masterFingerprint, ck.deriv));
                            keystore.setExtendedPublicKey(ExtendedKey.fromDescriptor(ck.xpub));

                            return keystore;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ImportException(e);
        }

        throw new ImportException("Correct derivation not found for script type: " + scriptType);
    }

    @Override
    public String getWalletImportDescription() {
        return getKeystoreImportDescription();
    }

    @Override
    public Wallet importWallet(InputStream inputStream, String password) throws ImportException {
        //Use default of P2WPKH
        Keystore keystore = getKeystore(ScriptType.P2WPKH, inputStream, "");

        Wallet wallet = new Wallet();
        wallet.setPolicyType(PolicyType.SINGLE);
        wallet.setScriptType(ScriptType.P2WPKH);
        wallet.getKeystores().add(keystore);
        wallet.setDefaultPolicy(Policy.getPolicy(PolicyType.SINGLE, ScriptType.P2WPKH, wallet.getKeystores(), null));

        if(!wallet.isValid()) {
            throw new ImportException("Wallet is in an inconsistent state.");
        }

        return wallet;
    }

    private static class ColdcardKeystore {
        public String deriv;
        public String name;
        public String xpub;
        public String xfp;
    }
}
