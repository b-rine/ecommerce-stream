"""Data models for ecommerce events"""

from enum import Enum
from datetime import datetime
from typing import Optional, Dict, Any
from pydantic import BaseModel, Field


class EventType(str, Enum):
    """Ecommerce event types"""
    PAGE_VIEW = "page_view"
    ADD_TO_CART = "add_to_cart"
    REMOVE_FROM_CART = "remove_from_cart"
    CHECKOUT_START = "checkout_start"
    PURCHASE = "purchase"
    PRODUCT_REVIEW = "product_review"


class Location(BaseModel):
    """User location information"""
    city: str
    country: str
    postal_code: str


class User(BaseModel):
    """User information"""
    user_id: str
    email: str
    name: str
    location: Location


class Product(BaseModel):
    """Product information"""
    product_id: str
    product_name: str
    category: str
    price: float
    brand: str


class ShippingAddress(BaseModel):
    """Shipping address for purchases"""
    street: str
    city: str
    state: str
    zip_code: str


class EcommerceEvent(BaseModel):
    """Base ecommerce event model"""
    event_id: str
    event_type: EventType
    timestamp: str = Field(default_factory=lambda: datetime.utcnow().isoformat())
    user: User
    product: Optional[Product] = None
    
    # Event-specific fields
    quantity: Optional[int] = None
    order_id: Optional[str] = None
    total_amount: Optional[float] = None
    payment_method: Optional[str] = None
    shipping_address: Optional[ShippingAddress] = None
    cart_total: Optional[float] = None
    item_count: Optional[int] = None
    rating: Optional[int] = None
    review_text: Optional[str] = None
    
    class Config:
        use_enum_values = True


class Statistics(BaseModel):
    """Statistics aggregation model"""
    total_events: int = 0
    events_by_type: Dict[str, int] = Field(default_factory=dict)
    total_revenue: float = 0.0
    total_orders: int = 0
    unique_users: int = 0
    unique_products: int = 0
    category_sales: Dict[str, float] = Field(default_factory=dict)
    revenue_over_time: list = Field(default_factory=list)  # List of (timestamp, revenue) tuples
    events_per_minute: list = Field(default_factory=list)  # List of (timestamp, count) tuples


class StreamStatus(BaseModel):
    """Stream status model"""
    producer_running: bool = False
    consumer_running: bool = False
    events_published: int = 0
    events_processed: int = 0
