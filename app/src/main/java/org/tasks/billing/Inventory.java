package org.tasks.billing;

import org.tasks.LocalBroadcastManager;
import org.tasks.preferences.Preferences;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class Inventory {

  private static final String SKU_VIP = "vip";
  public static final String SKU_TASKER = "tasker";
  public static final String SKU_THEMES = "themes";

  private final Preferences preferences;
  private final SignatureVerifier signatureVerifier;
  private final LocalBroadcastManager localBroadcastManager;

  private final Map<String, Purchase> purchases = new HashMap<>();
  private Purchase subscription = null;

  @Inject
  public Inventory(
      Preferences preferences,
      SignatureVerifier signatureVerifier,
      LocalBroadcastManager localBroadcastManager) {
    this.preferences = preferences;
    this.signatureVerifier = signatureVerifier;
    this.localBroadcastManager = localBroadcastManager;
    for (Purchase purchase : preferences.getPurchases()) {
      verifyAndAdd(purchase);
    }
  }

  public void clear() {
    Timber.d("clear()");
    purchases.clear();
  }

  public void add(Iterable<Purchase> purchases) {
    for (Purchase purchase : purchases) {
      verifyAndAdd(purchase);
    }
    preferences.setPurchases(this.purchases.values());
    localBroadcastManager.broadcastPurchasesUpdated();
  }

  private void verifyAndAdd(Purchase purchase) {
    if (signatureVerifier.verifySignature(purchase)) {
      Timber.d("add(%s)", purchase);
      purchases.put(purchase.getSku(), purchase);
      if (purchase.isProSubscription() && (subscription == null || subscription.isCanceled())) {
        subscription = purchase;
      }
    }
  }

  public boolean purchasedTasker() {
    return hasPro() || purchases.containsKey(SKU_TASKER);
  }

  public boolean purchasedThemes() {
    return hasPro() || purchases.containsKey(SKU_THEMES);
  }

  public Purchase getSubscription() {
    return subscription;
  }

  public boolean hasPro() {
    return true;
//    return subscription != null
//        || purchases.containsKey(SKU_VIP)
//        || BuildConfig.FLAVOR.equals("generic")
//        || (BuildConfig.DEBUG && preferences.getBoolean(R.string.p_debug_pro, false));
  }

  boolean purchased(String sku) {
    return purchases.containsKey(sku);
  }

  public Purchase getPurchase(String sku) {
    return purchases.get(sku);
  }
}
