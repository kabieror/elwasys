
ALTER TABLE devices
ALTER COLUMN fhem_name SET DEFAULT '';

ALTER TABLE devices
ALTER COLUMN fhem_switch_name SET DEFAULT '';

ALTER TABLE devices
ALTER COLUMN fhem_power_name SET DEFAULT '';

ALTER TABLE devices
ADD COLUMN deconz_id INT;

UPDATE config SET value='0.4.0' WHERE key='db.version';
