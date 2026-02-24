from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal


@dataclass(frozen=True)
class MaterialPriceItem:
    material_type: str
    source_name: str
    source_url: str
    effective_date: date
    fetched_at: datetime
    source_price: Decimal
    source_unit: str
    currency: str

    @property
    def price_per_kg(self) -> Decimal:
        if self.source_unit == "CNY/TON":
            return (self.source_price / Decimal("1000")).quantize(Decimal("0.01"))
        if self.source_unit == "CNY/KG":
            return self.source_price.quantize(Decimal("0.01"))
        raise ValueError(f"unsupported unit: {self.source_unit}")
