"""Event generation logic using Faker"""

import random
from typing import List, Optional
from faker import Faker
from datetime import datetime

from .models import (
    EcommerceEvent, EventType, User, Product, Location, ShippingAddress
)


fake = Faker()

# Product categories
CATEGORIES = [
    'Electronics', 'Clothing', 'Books', 'Home & Garden',
    'Sports', 'Toys', 'Beauty', 'Food & Beverages'
]

# Event type probability distribution
EVENT_TYPE_PROBABILITIES = {
    EventType.PAGE_VIEW: 0.40,
    EventType.ADD_TO_CART: 0.25,
    EventType.REMOVE_FROM_CART: 0.05,
    EventType.CHECKOUT_START: 0.15,
    EventType.PURCHASE: 0.10,
    EventType.PRODUCT_REVIEW: 0.05,
}

# Payment methods
PAYMENT_METHODS = ['credit_card', 'debit_card', 'paypal', 'apple_pay', 'google_pay']


class EventGenerator:
    """Generates realistic ecommerce events"""
    
    def __init__(self, num_users: int = 100, num_products: int = 50):
        """Initialize event generator with user and product pools"""
        self.users: List[User] = []
        self.products: List[Product] = []
        self._generate_user_pool(num_users)
        self._generate_product_pool(num_products)
    
    def _generate_user_pool(self, count: int):
        """Generate a pool of users"""
        self.users = []
        for _ in range(count):
            user = User(
                user_id=fake.uuid4(),
                email=fake.email(),
                name=fake.name(),
                location=Location(
                    city=fake.city(),
                    country=fake.country(),
                    postal_code=fake.postalcode()
                )
            )
            self.users.append(user)
    
    def _generate_product_pool(self, count: int):
        """Generate a pool of products"""
        self.products = []
        for _ in range(count):
            product = Product(
                product_id=fake.uuid4(),
                product_name=fake.catch_phrase(),
                category=random.choice(CATEGORIES),
                price=round(random.uniform(9.99, 999.99), 2),
                brand=fake.company()
            )
            self.products.append(product)
    
    def select_event_type(self) -> EventType:
        """Select an event type based on probability distribution"""
        rand = random.random()
        cumulative = 0.0
        for event_type, probability in EVENT_TYPE_PROBABILITIES.items():
            cumulative += probability
            if rand <= cumulative:
                return event_type
        return EventType.PAGE_VIEW  # fallback
    
    def generate_event(self, event_type: Optional[EventType] = None) -> EcommerceEvent:
        """Generate a random ecommerce event"""
        if event_type is None:
            event_type = self.select_event_type()
        
        # Select random user and product
        user = random.choice(self.users)
        product = random.choice(self.products) if self._requires_product(event_type) else None
        
        # Create base event
        event_data = {
            'event_id': fake.uuid4(),
            'event_type': event_type,
            'timestamp': datetime.utcnow().isoformat(),
            'user': user,
            'product': product
        }
        
        # Add event-specific data
        if event_type == EventType.ADD_TO_CART:
            event_data['quantity'] = random.randint(1, 5)
        
        elif event_type == EventType.REMOVE_FROM_CART:
            event_data['quantity'] = random.randint(1, 3)
        
        elif event_type == EventType.PURCHASE:
            quantity = random.randint(1, 3)
            total_amount = round(product.price * quantity, 2) if product else round(random.uniform(10.0, 500.0), 2)
            event_data.update({
                'order_id': fake.uuid4(),
                'quantity': quantity,
                'total_amount': total_amount,
                'payment_method': random.choice(PAYMENT_METHODS),
                'shipping_address': ShippingAddress(
                    street=fake.street_address(),
                    city=fake.city(),
                    state=fake.state(),
                    zip_code=fake.zipcode()
                )
            })
        
        elif event_type == EventType.CHECKOUT_START:
            event_data.update({
                'cart_total': round(random.uniform(25.0, 500.0), 2),
                'item_count': random.randint(1, 10)
            })
        
        elif event_type == EventType.PRODUCT_REVIEW:
            event_data.update({
                'rating': random.randint(1, 5),
                'review_text': fake.text(max_nb_chars=200)
            })
        
        return EcommerceEvent(**event_data)
    
    def _requires_product(self, event_type: EventType) -> bool:
        """Check if event type requires a product"""
        return event_type in [
            EventType.PAGE_VIEW,
            EventType.ADD_TO_CART,
            EventType.REMOVE_FROM_CART,
            EventType.PURCHASE,
            EventType.PRODUCT_REVIEW
        ]
