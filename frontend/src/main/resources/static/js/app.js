// IMS UI Application JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    var popoverList = popoverTriggerList.map(function(popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // Auto-dismiss alerts after 5 seconds
    setTimeout(function() {
        var alerts = document.querySelectorAll('.alert-dismissible');
        alerts.forEach(function(alert) {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);

    // Form validation
    var forms = document.querySelectorAll('.needs-validation');
    Array.prototype.slice.call(forms).forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

    // Confirm delete actions
    document.querySelectorAll('[data-confirm-delete]').forEach(function(element) {
        element.addEventListener('click', function(event) {
            if (!confirm('Are you sure you want to delete this item? This action cannot be undone.')) {
                event.preventDefault();
                return false;
            }
        });
    });

    // Number format helpers
    document.querySelectorAll('.currency-input').forEach(function(input) {
        input.addEventListener('blur', function() {
            const value = parseFloat(this.value);
            if (!isNaN(value)) {
                this.value = value.toFixed(2);
            }
        });
    });

    // Stock level indicators
    updateStockIndicators();
});

function updateStockIndicators() {
    document.querySelectorAll('[data-stock-level]').forEach(function(element) {
        const stock = parseInt(element.dataset.stockLevel);
        element.classList.remove('bg-success', 'bg-warning', 'bg-danger');

        if (stock < 10) {
            element.classList.add('bg-danger');
        } else if (stock < 50) {
            element.classList.add('bg-warning');
        } else {
            element.classList.add('bg-success');
        }
    });
}

// Order form helpers
function addOrderItem() {
    const container = document.getElementById('orderItems');
    const index = container.children.length;

    const itemHtml = `
        <div class="order-item-row mb-3 border rounded p-3" data-item-index="${index}">
            <div class="row">
                <div class="col-md-4">
                    <label class="form-label">Product</label>
                    <select class="form-select" name="orderItems[${index}].productDto.productId" required>
                        <option value="">Select Product</option>
                        <!-- Products will be populated by server -->
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Quantity</label>
                    <input type="number" class="form-control" name="orderItems[${index}].quantity" min="1" value="1" required>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Price</label>
                    <input type="number" class="form-control currency-input" name="orderItems[${index}].priceAtOrderTime" step="0.01" min="0" required>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="button" class="btn btn-danger btn-sm" onclick="removeOrderItem(this)">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', itemHtml);
    updateOrderItemIndices();
}

function removeOrderItem(button) {
    button.closest('.order-item-row').remove();
    updateOrderItemIndices();
}

function updateOrderItemIndices() {
    document.querySelectorAll('.order-item-row').forEach(function(row, index) {
        row.dataset.itemIndex = index;

        // Update input names
        row.querySelectorAll('input, select').forEach(function(input) {
            const name = input.name;
            if (name && name.includes('orderItems[')) {
                input.name = name.replace(/orderItems\[\d+\]/, `orderItems[${index}]`);
            }
        });
    });
}

// Utility functions
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

function formatDate(date) {
    return new Intl.DateTimeFormat('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(new Date(date));
}

/, orderItems[${index}]); } }); }); }$

// Utility functions function formatCurrency(amount) { return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount); }

function formatDate(date) {
    return new Intl.DateTimeFormat('en-US', {
       year: 'numeric',
       month: 'short',
       day: 'numeric',
       hour: '2-digit',
       minute: '2-digit'
    }).format(new Date(date));
}