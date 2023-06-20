package org.kabieror.elwasys.raspiclient.configuration;

import org.kabieror.elwasys.common.Location;
import org.kabieror.elwasys.common.LocationOccupiedException;
import org.kabieror.elwasys.raspiclient.application.ElwaManager;
import org.kabieror.elwasys.raspiclient.application.ICloseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dieser Manager aktualisiert regelmäßig die Registrierung des Clients auf
 * seinem Ort in der Datenbank.
 *
 * @author Oliver Kabierschke
 *
 */
public class LocationManager implements ICloseListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Location location;

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public LocationManager(WashguardConfiguration config)
            throws SQLException, LocationOccupiedException {
        this.location = ElwaManager.instance.getDataRetriever().getLocation(config.getLocationName());

        if (this.location == null) {
            this.logger.error("The given location '" + config.getLocationName()
                    + "' does not exist in the database.");
            throw new SQLException("Der Ort '" + config.getLocationName()
                    + "' kann in der Datenbank nicht gefunden werden.");
        }

        this.location.registerClient(config.getUid());

        ElwaManager.instance.listenToCloseEvent(this);

        this.service.scheduleAtFixedRate(() -> {
            try {
                this.location.registerClient(config.getUid());
            } catch (final Exception e) {
                this.logger.error("Could not update the location.", e);
            }
        } , 0l, 5l, TimeUnit.MINUTES);
    }

    @Override
    public void onClose(boolean restart) {
        this.service.shutdownNow();

        try {
            this.location.releaseLocation();
        } catch (final SQLException e) {
            this.logger.error("Could not release the registration on the location.", e);
        }
    }


}
