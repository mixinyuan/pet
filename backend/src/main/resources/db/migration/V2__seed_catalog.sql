INSERT INTO category (id, code, name, description, sort_order, status) VALUES
(1, 'all', '全部', '今日精选', 0, 'ACTIVE'),
(2, 'cat', '猫咪零食', '冻干与肉泥', 10, 'ACTIVE'),
(3, 'dog', '狗狗零食', '奖励与咀嚼', 20, 'ACTIVE'),
(4, 'freeze', '冻干', '高蛋白原切', 30, 'ACTIVE'),
(5, 'dental', '洁齿零食', '清新口气', 40, 'ACTIVE'),
(6, 'training', '训练奖励', '小颗粒易喂', 50, 'ACTIVE');

INSERT INTO product (id, sku, name, pet_type, flavor, price, rating, stock, status, image_url, description) VALUES
(1, 'PB-CHICKEN-FREEZE', '原切鸡肉冻干', '猫狗通用', '鸡胸肉', 39.00, 4.9, 120, 'ACTIVE', 'https://images.unsplash.com/photo-1601758125946-6ec2ef64daf8?auto=format&fit=crop&w=700&q=80', '原切鸡胸肉冻干，高蛋白、低负担，适合作为日常奖励或拌粮。'),
(2, 'PB-BEEF-TRAINING', '牛肉训练奖励粒', '狗狗适用', '牛肉南瓜', 32.00, 4.8, 100, 'ACTIVE', 'https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?auto=format&fit=crop&w=700&q=80', '小颗粒训练奖励，便携不油手，适合外出、召回和基础训练。'),
(3, 'PB-COD-STRIP', '鳕鱼鲜肉条', '猫咪适用', '深海鳕鱼', 28.00, 4.7, 90, 'ACTIVE', 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=700&q=80', '鱼肉条配方短，适口性好，适合猫咪日常加餐。'),
(4, 'PB-DENTAL-STICK', '薄荷洁齿棒', '狗狗适用', '鸡肉薄荷', 45.00, 4.8, 80, 'ACTIVE', 'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=700&q=80', '咀嚼型洁齿棒，兼顾口气管理和精力消耗。'),
(5, 'PB-DUCK-FREEZE', '鸭肉梨粒冻干', '猫狗通用', '鸭肉雪梨', 42.00, 4.9, 110, 'ACTIVE', 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=700&q=80', '鸭肉搭配雪梨，低敏清爽，适合猫狗通用奖励。'),
(6, 'PB-SALMON-BITES', '三文鱼软粒', '猫咪适用', '三文鱼', 36.00, 4.6, 95, 'ACTIVE', 'https://images.unsplash.com/photo-1596854407944-bf87f6fdd49e?auto=format&fit=crop&w=700&q=80', '三文鱼软粒，颗粒小、入口软，适合猫咪训练和日常奖励。'),
(7, 'PB-GOAT-MILK', '羊奶酪脆块', '猫狗通用', '羊奶酪', 30.00, 4.7, 130, 'ACTIVE', 'https://images.unsplash.com/photo-1583511655826-05700d52f4d9?auto=format&fit=crop&w=700&q=80', '羊奶酪风味脆块，香脆补钙，猫狗都能轻松入口。'),
(8, 'PB-RABBIT-DENTAL', '兔肉洁齿小棒', '小型犬适用', '兔肉胡萝卜', 49.00, 4.8, 70, 'ACTIVE', 'https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?auto=format&fit=crop&w=700&q=80', '低敏兔肉洁齿小棒，适合小型犬咀嚼护理。');

INSERT INTO product_category_rel (product_id, category_id) VALUES
(1, 2), (1, 3), (1, 4),
(2, 3), (2, 6),
(3, 2), (3, 6),
(4, 3), (4, 5),
(5, 2), (5, 3), (5, 4),
(6, 2), (6, 6),
(7, 2), (7, 3),
(8, 3), (8, 5);

INSERT INTO product_tag (id, name, status) VALUES
(1, '热销', 'ACTIVE'),
(2, '高蛋白', 'ACTIVE'),
(3, '训练', 'ACTIVE'),
(4, '低脂', 'ACTIVE'),
(5, '无谷', 'ACTIVE'),
(6, '新品', 'ACTIVE'),
(7, '洁齿', 'ACTIVE'),
(8, '清新', 'ACTIVE'),
(9, '低敏', 'ACTIVE'),
(10, '亮毛', 'ACTIVE'),
(11, '小颗粒', 'ACTIVE'),
(12, '补钙', 'ACTIVE'),
(13, '香脆', 'ACTIVE');

INSERT INTO product_tag_rel (product_id, tag_id) VALUES
(1, 1), (1, 2),
(2, 3), (2, 4),
(3, 5), (3, 6),
(4, 7), (4, 8),
(5, 9), (5, 1),
(6, 10), (6, 11),
(7, 12), (7, 13),
(8, 7), (8, 9);

INSERT INTO bundle (id, name, description, price, original_price, discount_label, status, sort_order) VALUES
(1, '新手尝鲜包', '冻干、肉条、洁齿棒各一份，适合第一次购买。', 89.00, 108.00, '省 18%', 'ACTIVE', 10),
(2, '训练奖励包', '小颗粒高适口零食，外出、召回和定点训练更顺手。', 76.00, 98.00, '省 22%', 'ACTIVE', 20),
(3, '洁齿护理包', '洁齿棒与低脂咀嚼零食组合，兼顾口气与消耗精力。', 96.00, 113.00, '省 15%', 'ACTIVE', 30);

INSERT INTO bundle_item (bundle_id, product_id, quantity) VALUES
(1, 1, 1), (1, 3, 1), (1, 4, 1),
(2, 2, 2), (2, 6, 1),
(3, 4, 1), (3, 8, 1);

INSERT INTO review (product_id, buyer_name, pet_name, pet_breed, content, avatar_url, purchased_product, rating, sort_order, status) VALUES
(1, '橘子', '橘子', '英短', '鸡肉冻干一打开就冲过来，颗粒大小刚好，拌粮也很方便。', 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=360&q=80', '原切鸡肉冻干', 5.0, 10, 'ACTIVE'),
(2, '豆包', '豆包', '柴犬', '训练奖励包很实用，不油手，出门带一小袋就够了。', 'https://images.unsplash.com/photo-1558788353-f76d92427f16?auto=format&fit=crop&w=360&q=80', '牛肉训练粒', 4.9, 20, 'ACTIVE'),
(3, 'Momo', 'Momo', '布偶', '鱼肉条味道不冲，配料表很短，家里两只都爱吃。', 'https://images.unsplash.com/photo-1592194996308-7b43878e84a6?auto=format&fit=crop&w=360&q=80', '鳕鱼鲜肉条', 4.8, 30, 'ACTIVE');
