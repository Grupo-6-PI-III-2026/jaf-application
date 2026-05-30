-- Update admin password to known value: Admin@123
UPDATE funcionario 
SET senha = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi' 
WHERE email = 'admin@gmail.com';