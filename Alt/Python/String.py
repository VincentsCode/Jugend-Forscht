data = "1200,200"
breakpoint = data.index(",")
angle = data[0:breakpoint]
distance = data[breakpoint +1:len(data)]

print "Angle: " + angle + "\n"
print "Distance: " + distance + "\n"