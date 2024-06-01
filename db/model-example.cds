namespace example;

using {
    cuid,
    managed
} from '@sap/cds/common';


entity table01 : cuid, managed {
    field_str01  : String(10);
    field_str02  : String(20);
    field_dec_01 : Decimal(13, 3);
}
