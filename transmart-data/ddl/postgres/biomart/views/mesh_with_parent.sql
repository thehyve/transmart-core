--
-- Name: mesh_with_parent; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW mesh_with_parent AS
 SELECT mesh.mn,
    mesh.ui,
    mesh.mh,
        CASE
            WHEN (biomart_user.instr(mesh.mn, '.'::character varying) = 0) THEN NULL::text
            ELSE substr((mesh.mn)::text, 1, (biomart_user.instr(mesh.mn, '.'::character varying, '-1'::integer) - 1))
        END AS pn
   FROM mesh
  ORDER BY mesh.mn;

