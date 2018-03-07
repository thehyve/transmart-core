import inc.oracle.SqlProducer

def sql = SqlProducer.createFromEnv()
sql.execute '''
BEGIN
DBMS_SNAPSHOT.REFRESH( '"BIOMART"."BIO_MARKER_CORREL_MV"','C');
END;
'''