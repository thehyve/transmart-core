
-- Update visual attributes of leaf nodes to include variable type

update i2b2_secure set c_visualattributes = 'LAC'
where (c_visualattributes = 'LA ' or c_visualattributes = 'LA') and
not c_metadataxml like '%<Oktousevalues>Y</Oktousevalues>%';

update i2b2_secure set c_visualattributes = 'LAN'
where (c_visualattributes = 'LA ' or c_visualattributes = 'LA') and
c_metadataxml like '%<Oktousevalues>Y</Oktousevalues>%';
