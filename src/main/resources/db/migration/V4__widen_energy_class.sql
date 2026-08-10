-- energy_class dostał VARCHAR(3), bo długość dobrano pod etykietę („A+"),
-- podczas gdy Hibernate zapisuje nazwę stałej enuma — a ta dla klasy A+ brzmi
-- `A_PLUS` i ma 6 znaków. Zapis oferty z tą klasą kończył się błędem
-- „value too long for type character varying(3)".
--
-- `ddl-auto: validate` tego nie łapie: Hibernate sprawdza istnienie i typ
-- kolumny, ale nie porównuje jej długości z najdłuższą nazwą w enumie.
ALTER TABLE properties
    ALTER COLUMN energy_class TYPE VARCHAR(10);
